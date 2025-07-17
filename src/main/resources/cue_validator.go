package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"C"
	"encoding/json"
	"fmt"
	"strings"

	"cuelang.org/go/cue"
	"cuelang.org/go/cue/cuecontext"
)

//export ValidateJSONWithCue
func ValidateJSONWithCue(schemaStr *C.char, jsonStr *C.char) *C.char {
	schema := C.GoString(schemaStr)
	jsonData := C.GoString(jsonStr)

	ctx := cuecontext.New()

	// Compile schema
	cueSchemaVal := ctx.CompileString(schema)
	if cueSchemaVal.Err() != nil {
		return C.CString(fmt.Sprintf(`{"error": "Invalid schema: %s"}`, cueSchemaVal.Err()))
	}

	// Compile JSON data
	cueJSONVal := ctx.CompileString(jsonData)
	if cueJSONVal.Err() != nil {
		return C.CString(fmt.Sprintf(`{"error": "Invalid JSON: %s"}`, cueJSONVal.Err()))
	}

	schemaVal := cueSchemaVal.LookupPath(cue.ParsePath("Request"))
	jsonVal := cueJSONVal.LookupPath(cue.ParsePath("Request"))

	resultMap := make(map[string]string)
	validateRecursive("Request", schemaVal, jsonVal, &resultMap)

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

	if schemaKind == cue.ListKind && dataKind == cue.ListKind {
		vals := extractListValues(dataField)
		listLevelMsg := applyListValidations(schemaField, vals)

		if listLevelMsg != "" {
			(*resultMap)[fullPath] = listLevelMsg
		} else {
			(*resultMap)[fullPath] = "valid"
		}

		// Skip individual item validation if list-level validations are present
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

	if schemaKind == cue.StructKind && dataKind == cue.StructKind {
		fields, _ := schemaField.Fields()
		for fields.Next() {
			field := fields.Selector().String()
			validateRecursive(fmt.Sprintf("%s.%s", path, field),
				schemaField.LookupPath(cue.ParsePath(field)),
				dataField.LookupPath(cue.ParsePath(field)),
				resultMap,
			)
		}
		return
	}

	validateScalar(fullPath, schemaField, dataField, resultMap)
}

func validateScalar(field string, schemaField, dataField cue.Value, resultMap *map[string]string) {
	if err := schemaField.Unify(dataField).Validate(); err != nil {
		msg := getCustomMessage(schemaField)
		if msg == "" {
			msg = err.Error()
		}
		(*resultMap)[field] = msg
	} else {
		(*resultMap)[field] = "valid"
	}
}

func getCustomMessage(v cue.Value) string {
	if attr := v.Attribute("tag"); attr.Err() == nil {
		if msg, found, _ := attr.Lookup(0, "message"); found {
			return strings.Trim(msg, `"`)
		}
	}
	return ""
}

func extractListValues(data cue.Value) []string {
	list := []string{}
	iter, _ := data.List()
	for iter.Next() {
		val := iter.Value()
		switch val.IncompleteKind() {
		case cue.StringKind:
			strVal, _ := val.String()
			list = append(list, strVal)
		case cue.IntKind:
			intVal, _ := val.Int64()
			list = append(list, fmt.Sprintf("%d", intVal))
		case cue.NumberKind:
			numVal, _ := val.Float64()
			list = append(list, fmt.Sprintf("%f", numVal))
		}
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
			return fmt.Sprintf("%s Values : [%s]", msg, strings.Join(missing, ", "))
		}
	}

	if containsAny != nil && !containsAnyOne(values, containsAny) {
		return fmt.Sprintf("%s - Values : [%s]", msg, strings.Join(containsAny, ", "))
	}

	if notContains != nil {
		forbidden := intersect(values, notContains)
		if len(forbidden) > 0 {
			return fmt.Sprintf("%s - Values: %s", msg, strings.Join(forbidden, ", "))
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
		_, hasRequiredAll, _ := attr.Lookup(0, "required_all")
		_, hasContainsAny, _ := attr.Lookup(0, "contains_any")
		_, hasNotContains, _ := attr.Lookup(0, "not_contains")
		return hasRequiredAll || hasContainsAny || hasNotContains
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

func containsAll(haystack, needles []string) bool {
	set := make(map[string]struct{})
	for _, v := range haystack {
		set[v] = struct{}{}
	}
	for _, n := range needles {
		if _, ok := set[n]; !ok {
			return false
		}
	}
	return true
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
