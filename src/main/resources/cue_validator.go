package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"time"

	"cuelang.org/go/cue"
	"cuelang.org/go/cue/cuecontext"
	"cuelang.org/go/cue/format"
)

//export ValidateJSONWithCue
func ValidateJSONWithCue(schemaCStr, jsonCStr *C.char) *C.char {
	schemaStr := C.GoString(schemaCStr)
	jsonStr := C.GoString(jsonCStr)

	ctx := cuecontext.New()
	resultMap := make(map[string]string)

	schemaVal := ctx.CompileString(schemaStr)
	if err := schemaVal.Err(); err != nil {
		return mapToCString(map[string]string{"error": "Invalid CUE schema: " + err.Error()})
	}

	jsonVal := ctx.CompileString(jsonStr)
	if err := jsonVal.Err(); err != nil {
		return mapToCString(map[string]string{"error": "Invalid JSON input: " + err.Error()})
	}

	topKey, err := getTopLevelKey(jsonStr)
	if err != nil {
		return mapToCString(map[string]string{"error": "Invalid JSON input: " + err.Error()})
	}
	schemaRoot := schemaVal.LookupPath(cue.ParsePath(topKey))
	jsonRoot := jsonVal.LookupPath(cue.ParsePath(topKey))

	if !schemaRoot.Exists() {
		return mapToCString(map[string]string{"error": "Schema must define a 'Request' root object"})
	}

	validateRecursive("", schemaRoot, jsonRoot, &resultMap)

	return mapToCString(resultMap)
}

func getTopLevelKey(jsonStr string) (string, error) {
	var top map[string]interface{}
	if err := json.Unmarshal([]byte(jsonStr), &top); err != nil {
		return "", err
	}
	for k := range top {
		return k, nil
	}
	return "", fmt.Errorf("empty JSON object")
}

func validateRecursive(path string, schema cue.Value, data cue.Value, resultMap *map[string]string) {
	iter, _ := schema.Fields()
	for iter.Next() {
		fieldName := iter.Label()
		fullPath := fieldName
		if path != "" {
			fullPath = path + "." + fieldName
		}

		schemaField := iter.Value()
		dataField := data.LookupPath(cue.ParsePath(fieldName))

		if !dataField.Exists() {
			continue
		}

		if schemaField.IncompleteKind() == cue.StructKind {
			validateRecursive(fullPath, schemaField, dataField, resultMap)
			continue
		}

		if schemaField.IncompleteKind() == cue.ListKind {
			vals := extractListValues(dataField)
			if err := applyListValidations(schemaField, vals); err != "" {
				(*resultMap)[fullPath] = err
			} else {
				(*resultMap)[fullPath] = "valid"
			}
			continue
		}

		validateScalar(fullPath, schemaField, dataField, resultMap)
	}
}

func validateScalar(path string, schemaField, dataField cue.Value, resultMap *map[string]string) {
	decimalPlaces := getDecimalPlaces(schemaField)
	dateLayout := getDateLayout(schemaField)

	result := schemaField.Unify(dataField)
	if err := result.Validate(); err != nil {
		msg := getCustomMessage(schemaField)
		(*resultMap)[path] = msg
		return
	}

	if decimalPlaces >= 0 {
		raw := formatNodeToString(dataField)
		if strings.Contains(raw, `"`) {
			raw = strings.Trim(raw, `"`)
		}
		if !hasExactDecimalPlaces(raw, decimalPlaces) {
			msg := getCustomMessage(schemaField)
			(*resultMap)[path] = fmt.Sprintf("%s must have exactly %d decimal places", msg, decimalPlaces)
			return
		}
	}

	if dateLayout != "" {
		strVal, err := dataField.String()
		if err != nil || !isValidDate(strVal, dateLayout) {
			msg := getCustomMessage(schemaField)
			(*resultMap)[path] = fmt.Sprintf("%s expected format %s", msg, dateLayout)
			return
		}
	}

	(*resultMap)[path] = "valid"
}

func applyListValidations(field cue.Value, input []string) string {
	msg := getCustomMessage(field)

	if len(input) == 0 {
		return "Request having empty Array or List"
	}
	reqAll := getTagList(field, "required_all")
	if len(reqAll) > 0 && !containsAll(input, reqAll) {
		return fmt.Sprintf("%s: must contain all values [%s]", msg, strings.Join(reqAll, ", "))
	}

	containsAnyVals := getTagList(field, "contains_any")
	if len(containsAnyVals) > 0 && !containsAny(input, containsAnyVals) {
		return fmt.Sprintf("%s: must contain at least one of [%s]", msg, strings.Join(containsAnyVals, ", "))
	}

	notContain := getTagList(field, "not_contains")
	if len(notContain) > 0 && containsAny(input, notContain) {
		return fmt.Sprintf("%s: must not contain any of [%s]", msg, strings.Join(notContain, ", "))
	}

	return ""
}

func extractListValues(v cue.Value) []string {
	var list []string
	iter, _ := v.List()
	for iter.Next() {
		val := iter.Value()
		str := formatNodeToString(val)
		list = append(list, strings.Trim(str, `"`))
	}
	return list
}

func getTagList(v cue.Value, tagName string) []string {
	if attr := v.Attribute("tag"); attr.Err() == nil {
		if val, found, err := attr.Lookup(0, tagName); err == nil && found {
			return strings.Split(strings.Trim(val, `"`), ",")
		}
	}
	return nil
}

func containsAll(input, required []string) bool {
	set := make(map[string]bool)
	for _, val := range input {
		set[val] = true
	}
	for _, r := range required {
		if !set[r] {
			return false
		}
	}
	return true
}

func containsAny(input, targets []string) bool {
	set := make(map[string]bool)
	for _, val := range input {
		set[val] = true
	}
	for _, t := range targets {
		if set[t] {
			return true
		}
	}
	return false
}

func getDecimalPlaces(v cue.Value) int {
	if attr := v.Attribute("tag"); attr.Err() == nil {
		if val, found, err := attr.Lookup(0, "decimal"); err == nil && found {
			if d, err := strconv.Atoi(val); err == nil {
				return d
			}
		}
	}
	return -1
}

func getDateLayout(v cue.Value) string {
	if attr := v.Attribute("tag"); attr.Err() == nil {
		if val, found, err := attr.Lookup(0, "date"); err == nil && found {
			return convertDateFormat(val)
		}
	}
	return ""
}

func isValidDate(value string, layout string) bool {
	layout = convertDateFormat(layout)
	_, err := time.Parse(layout, value)
	return err == nil
}

func hasExactDecimalPlaces(str string, places int) bool {
	dot := strings.Index(str, ".")
	if dot == -1 {
		return places == 0
	}
	decimals := str[dot+1:]
	return len(decimals) == places
}

func convertDateFormat(layout string) string {
	replacer := strings.NewReplacer(
		"yyyy", "2006",
		"MM", "01",
		"dd", "02",
		"HH", "15",
		"mm", "04",
		"ss", "05",
	)
	return replacer.Replace(layout)
}

func getCustomMessage(v cue.Value) string {
	if attr := v.Attribute("tag"); attr.Err() == nil {
		if val, found, err := attr.Lookup(0, "message"); err == nil && found {
			return strings.Trim(val, `"`)
		}
	}
	if attr := v.Attribute("message"); attr.Err() == nil {
		if val, err := attr.String(0); err == nil {
			return strings.Trim(val, `"`)
		}
	}
	return ""
}

func formatNodeToString(v cue.Value) string {
	syn := v.Syntax(
		cue.Final(),
		cue.Definitions(true),
		cue.Concrete(true),
	)
	b, err := format.Node(syn)
	if err != nil {
		return ""
	}
	return strings.TrimSpace(string(b))
}

func mapToCString(m map[string]string) *C.char {
	jsonBytes, _ := json.Marshal(m)
	return C.CString(string(jsonBytes))
}

func main() {}
