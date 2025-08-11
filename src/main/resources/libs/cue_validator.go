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
func ValidateJSONWithCue(schemaStr *C.char, jsonStr *C.char) *C.char {
	schema := C.GoString(schemaStr)
	jsonData := C.GoString(jsonStr)

	ctx := cuecontext.New()

	cueSchemaVal := ctx.CompileString(schema)
	if cueSchemaVal.Err() != nil {
		return C.CString(fmt.Sprintf(`{"error": "Invalid schema: %s"}`, cueSchemaVal.Err()))
	}

	cueJSONVal := ctx.CompileString(jsonData)
	if cueJSONVal.Err() != nil {
		return C.CString(fmt.Sprintf(`{"error": "Invalid JSON: %s"}`, cueJSONVal.Err()))
	}

	schemaVal := cueSchemaVal.LookupPath(cue.ParsePath("Request"))
	jsonVal := cueJSONVal.LookupPath(cue.ParsePath("Request"))

	resultMap := make(map[string]string)
	validateRecursive("", schemaVal, jsonVal, &resultMap)

	resultJSON, err := json.Marshal(resultMap)
	if err != nil {
		return C.CString(`{"error": "Failed to marshal result"}`)
	}

	return C.CString(string(resultJSON))
}

func validateRecursive(path string, schemaField, dataField cue.Value, resultMap *map[string]string) {
	schemaKind := schemaField.IncompleteKind()
	dataKind := dataField.IncompleteKind()
	fullPath := path

	// List handling
	if schemaKind == cue.ListKind && dataKind == cue.ListKind {
		vals := extractListValues(dataField)
		listLevelMsg := applyListValidations(schemaField, vals)

		if listLevelMsg != "" {
			(*resultMap)[fullPath] = listLevelMsg
		} else if hasListLevelTags(schemaField) {
			(*resultMap)[fullPath] = "valid"
		}

		if hasListLevelTags(schemaField) {
			return
		}

		listIter, _ := dataField.List()
		itemSchema := schemaField.LookupPath(cue.MakePath(cue.AnyIndex))

		i := 0
		for listIter.Next() {
			item := listIter.Value()
			itemPath := fmt.Sprintf("%s[%d]", fullPath, i)

			if itemSchema.Exists() {
				if itemSchema.IncompleteKind() == cue.StructKind {
					validateRecursive(itemPath, itemSchema, item, resultMap)
				} else {
					validateScalar(itemPath, itemSchema, item, resultMap)
				}
			} else {
				(*resultMap)[itemPath] = "Schema for list item not found"
			}
			i++
		}
		return
	}

	// Struct handling
	if schemaKind == cue.StructKind && dataKind == cue.StructKind {
		fields, _ := schemaField.Fields()
		for fields.Next() {
			field := fields.Selector().String()
			newPath := field
			if path != "" {
				newPath = path + "." + field
			}
			validateRecursive(newPath,
				schemaField.LookupPath(cue.ParsePath(field)),
				dataField.LookupPath(cue.ParsePath(field)),
				resultMap,
			)
		}
		return
	}

	// Scalar handling
	validateScalar(fullPath, schemaField, dataField, resultMap)
}

func validateScalar(field string, schemaField, dataField cue.Value, resultMap *map[string]string) {
	attr := schemaField.Attribute("tag")
	actualVal := extractCueValueAsString(dataField)

	// Null or empty check
	if actualVal == "" || actualVal == "null" {
		msg := getCustomMessage(schemaField)
		if msg == "" {
			msg = fmt.Sprintf("%s must not be null or empty", field)
		}
		(*resultMap)[field] = fmt.Sprintf("%s (Input Value: %s)", msg, actualVal)
		return
	}

	if attr.Err() == nil {
		// Decimal exact places
		if val, found, _ := attr.Lookup(0, "decimal"); found {
			expected, _ := strconv.Atoi(strings.Trim(val, `"`))
			actual := getDecimalPlaces(dataField)
			if actual != expected {
				msg := getCustomMessage(schemaField)
				if msg == "" {
					msg = fmt.Sprintf("Must have exactly %d decimal places", expected)
				}
				(*resultMap)[field] = fmt.Sprintf("%s (Input Value: %s)", msg, actualVal)
				return
			}
		}

		// Decimal max places
		if val, found, _ := attr.Lookup(0, "decimal_max"); found {
			max, _ := strconv.Atoi(strings.Trim(val, `"`))
			actual := getDecimalPlaces(dataField)
			if actual > max {
				msg := getCustomMessage(schemaField)
				if msg == "" {
					msg = fmt.Sprintf("Must have at most %d decimal places", max)
				}
				(*resultMap)[field] = fmt.Sprintf("%s (Input Value: %s)", msg, actualVal)
				return
			}
		}

		// Min characters
		if val, found, _ := attr.Lookup(0, "min_chars"); found {
			min, _ := strconv.Atoi(strings.Trim(val, `"`))
			if len(actualVal) < min {
				msg := getCustomMessage(schemaField)
				if msg == "" {
					msg = fmt.Sprintf("Must have at least %d characters", min)
				}
				(*resultMap)[field] = fmt.Sprintf("%s (Input Value: %s)", msg, actualVal)
				return
			}
		}

		// Max characters
		if val, found, _ := attr.Lookup(0, "max_chars"); found {
			max, _ := strconv.Atoi(strings.Trim(val, `"`))
			if len(actualVal) > max {
				msg := getCustomMessage(schemaField)
				if msg == "" {
					msg = fmt.Sprintf("Must have at most %d characters", max)
				}
				(*resultMap)[field] = fmt.Sprintf("%s (Input Value: %s)", msg, actualVal)
				return
			}
		}

		// Must contain substring
		if val, found, _ := attr.Lookup(0, "must_contain"); found {
			sub := strings.Trim(val, `"`)
			if !strings.Contains(actualVal, sub) {
				msg := getCustomMessage(schemaField)
				if msg == "" {
					msg = fmt.Sprintf("Must contain substring '%s'", sub)
				}
				(*resultMap)[field] = fmt.Sprintf("%s (Input Value: %s)", msg, actualVal)
				return
			}
		}

		// Must NOT contain substring
		if val, found, _ := attr.Lookup(0, "must_not_contain"); found {
			sub := strings.Trim(val, `"`)
			if strings.Contains(actualVal, sub) {
				msg := getCustomMessage(schemaField)
				if msg == "" {
					msg = fmt.Sprintf("Must not contain substring '%s'", sub)
				}
				(*resultMap)[field] = fmt.Sprintf("%s (Input Value: %s)", msg, actualVal)
				return
			}
		}

		// Date format validation
		if val, found, _ := attr.Lookup(0, "date"); found {
			expectedFormat := strings.Trim(val, `"`)
			dateStr, err := dataField.String()
			if err == nil && !validateDateFormat(dateStr, expectedFormat) {
				msg := getCustomMessage(schemaField)
				if msg == "" {
					msg = fmt.Sprintf("Invalid date format, expected: %s", expectedFormat)
				}
				(*resultMap)[field] = fmt.Sprintf("%s (Input Value: %s)", msg, actualVal)
				return
			}
		}
	}

	// CUE core validation
	if err := schemaField.Unify(dataField).Validate(); err != nil {
		msg := getCustomMessage(schemaField)
		if msg == "" {
			msg = err.Error()
		}
		(*resultMap)[field] = fmt.Sprintf("%s (Input Value: %s)", msg, actualVal)
		return
	}

	(*resultMap)[field] = "valid"
}

func getDecimalPlaces(val cue.Value) int {
	node := val.Syntax()
	b, err := format.Node(node)
	if err != nil {
		return 0
	}
	raw := string(b)
	raw = strings.Trim(raw, `"`)
	if !strings.Contains(raw, ".") {
		return 0
	}
	parts := strings.Split(raw, ".")
	if len(parts) != 2 {
		return 0
	}
	return len(parts[1])
}

func validateDateFormat(value, layout string) bool {
	goLayout := convertToGoLayout(layout)
	_, err := time.Parse(goLayout, value)
	return err == nil
}

func convertToGoLayout(format string) string {
	return strings.NewReplacer(
		"yyyy", "2006",
		"MM", "01",
		"dd", "02",
		"HH", "15",
		"mm", "04",
		"ss", "05",
	).Replace(format)
}

func getCustomMessage(v cue.Value) string {
	if attr := v.Attribute("tag"); attr.Err() == nil {
		if msg, found, _ := attr.Lookup(0, "message"); found {
			return strings.Trim(msg, `"`)
		}
	}
	return ""
}

func extractCueValueAsString(val cue.Value) string {
	syntax := val.Syntax()
	if b, err := format.Node(syntax); err == nil {
		return strings.Trim(string(b), `"`)
	}
	return "invalid"
}

func extractListValues(data cue.Value) []string {
	list := []string{}
	iter, _ := data.List()
	for iter.Next() {
		val := iter.Value()
		list = append(list, extractCueValueAsString(val))
	}
	return list
}

func applyListValidations(v cue.Value, values []string) string {
	requiredAll := getTagList(v, "required_all")
	containsAny := getTagList(v, "contains_any")
	notContains := getTagList(v, "not_contains")
	msg := getCustomMessage(v)
	if msg == "" {
		msg = "List validation failed"
	}

	if requiredAll != nil {
		missing := missingValues(values, requiredAll)
		if len(missing) > 0 {
			return fmt.Sprintf("%s - Missing: [%s], Input Value: [%s]", msg, strings.Join(missing, ", "), strings.Join(values, ", "))
		}
	}

	if containsAny != nil && !containsAnyOne(values, containsAny) {
		return fmt.Sprintf("%s - Expected any of [%s], Input Value: [%s]", msg, strings.Join(containsAny, ", "), strings.Join(values, ", "))
	}

	if notContains != nil {
		forbidden := intersect(values, notContains)
		if len(forbidden) > 0 {
			return fmt.Sprintf("%s - Invalid values found: [%s], Input Value: [%s]", msg, strings.Join(forbidden, ", "), strings.Join(values, ", "))
		}
	}

	return ""
}

func missingValues(actual, required []string) []string {
	missing := []string{}
	valueSet := make(map[string]bool)
	for _, val := range actual {
		valueSet[val] = true
	}
	for _, req := range required {
		if !valueSet[req] {
			missing = append(missing, req)
		}
	}
	return missing
}

func hasListLevelTags(v cue.Value) bool {
	if attr := v.Attribute("tag"); attr.Err() == nil {
		_, a, _ := attr.Lookup(0, "required_all")
		_, b, _ := attr.Lookup(0, "contains_any")
		_, c, _ := attr.Lookup(0, "not_contains")
		return a || b || c
	}
	return false
}

func intersect(list1, list2 []string) []string {
	set := make(map[string]bool)
	for _, v := range list1 {
		set[v] = true
	}
	intersection := []string{}
	for _, v := range list2 {
		if set[v] {
			intersection = append(intersection, v)
		}
	}
	return intersection
}

func getTagList(v cue.Value, tagName string) []string {
	if attr := v.Attribute("tag"); attr.Err() == nil {
		if val, found, err := attr.Lookup(0, tagName); err == nil && found {
			return strings.Split(strings.Trim(val, `"`), ",")
		}
	}
	return nil
}

func containsAnyOne(haystack, needles []string) bool {
	set := make(map[string]struct{})
	for _, v := range haystack {
		set[v] = struct{}{}
	}
	for _, n := range needles {
		if _, ok := set[n]; ok {
			return true
		}
	}
	return false
}

func main() {}
