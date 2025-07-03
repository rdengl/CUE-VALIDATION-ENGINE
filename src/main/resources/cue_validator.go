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
			listIter, err := dataField.List()
			if err != nil {
				(*resultMap)[fullPath] = "Invalid array"
				continue
			}
			schemaElem, _ := schemaField.Elem()
			index := 0
			for listIter.Next() {
				item := listIter.Value()
				itemPath := fmt.Sprintf("%s[%d]", fullPath, index)

				if schemaElem.IncompleteKind() == cue.StructKind {
					validateRecursive(itemPath, schemaElem, item, resultMap)
				} else {
					validateScalar(itemPath, schemaElem, item, resultMap)
				}
				index++
			}
			continue
		}

		validateScalar(fullPath, schemaField, dataField, resultMap)
	}
}

func validateScalar(path string, schemaField, dataField cue.Value, resultMap *map[string]string) {
	decimalPlaces := getDecimalPlaces(schemaField)
	dateLayout := getDateLayout(schemaField)

	// Step 1: CUE validation
	result := schemaField.Unify(dataField)
	if err := result.Validate(); err != nil {
		msg := getCustomMessage(schemaField)
		(*resultMap)[path] = msg
		return
	}

	// Step 2: Decimal check (avoid float64 error)
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

	// Step 3: Date format
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

func hasExactDecimalPlaces(str string, places int) bool {
	dot := strings.Index(str, ".")
	if dot == -1 {
		return places == 0
	}
	decimals := str[dot+1:]
	return len(decimals) == places
}

func isValidDate(value string, layout string) bool {
	layout = convertDateFormat(layout)
	_, err := time.Parse(layout, value)
	return err == nil
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
