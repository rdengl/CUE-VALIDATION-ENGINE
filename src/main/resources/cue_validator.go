package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"encoding/json"
	"fmt"
	"strings"

	"cuelang.org/go/cue"
	"cuelang.org/go/cue/cuecontext"
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

	schemaRoot := schemaVal.LookupPath(cue.ParsePath("Request"))
	jsonRoot := jsonVal.LookupPath(cue.ParsePath("Request"))

	if !schemaRoot.Exists() {
		return mapToCString(map[string]string{"error": "Schema must define a 'Request' root object"})
	}

	validateRecursive("", schemaRoot, jsonRoot, &resultMap)

	return mapToCString(resultMap)
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

		// Recurse if struct
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
					result := schemaElem.Unify(item)
					if err := result.Validate(); err != nil {
						msg := getCustomMessage(schemaElem)
						if msg != "" {
							(*resultMap)[itemPath] = msg
						}
					}
				}
				index++
			}
		}

		result := schemaField.Unify(dataField)
		if err := result.Validate(); err != nil {
			msg := getCustomMessage(schemaField)
			if msg != "" {
				(*resultMap)[fullPath] = msg
			}
		} else {
			(*resultMap)[fullPath] = "valid"
		}
	}
}

func getCustomMessage(schemaField cue.Value) string {
	// Try to fetch @tag(message="...")
	if attr := schemaField.Attribute("tag"); attr.Err() == nil {
		if val, found, err := attr.Lookup(0, "message"); err == nil && found {
			return strings.Trim(val, `"`)
		}
	}

	// Fallback to @message("...")
	if attr := schemaField.Attribute("message"); attr.Err() == nil {
		if str, err := attr.String(0); err == nil {
			return strings.Trim(str, `"`)
		}
	}

	return ""
}

func mapToCString(m map[string]string) *C.char {
	jsonBytes, _ := json.Marshal(m)
	return C.CString(string(jsonBytes))
}

func main() {}
