package main

/*
#include <stdlib.h>
*/
import "C"

import (
	"encoding/json"
	"fmt"
	"math"
	"strconv"
	"strings"
	"time"

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
		return k, nil // return first key
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

		// Recurse if struct
		if schemaField.IncompleteKind() == cue.StructKind {
			validateRecursive(fullPath, schemaField, dataField, resultMap)
			continue
		}

		// Handle array
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
					} else {
						if decPlaces := getDecimalPlacesFromTag(schemaElem); decPlaces >= 0 {
							if !hasExactDecimalPlaces(item, decPlaces) {
								msg := getCustomMessage(schemaElem)
								(*resultMap)[itemPath] = fmt.Sprintf("%s%d decimal places", msg, decPlaces)
								continue
							}
						}
						if dateLayout := getDateFormatFromTag(schemaElem); dateLayout != "" {
							goLayout := convertDateFormatToGo(dateLayout)
							if !isValidDateFormat(item, goLayout) {
								(*resultMap)[itemPath] = fmt.Sprintf("Invalid date format, expected %s", dateLayout)
								continue
							}
						}
						(*resultMap)[itemPath] = "valid"
					}
				}
				index++
			}
			continue
		}

		// Scalar validation
		result := schemaField.Unify(dataField)
		if err := result.Validate(); err != nil {
			msg := getCustomMessage(schemaField)
			if msg != "" {
				(*resultMap)[fullPath] = msg
			}
		} else {
			if decPlaces := getDecimalPlacesFromTag(schemaField); decPlaces >= 0 {
				if !hasExactDecimalPlaces(dataField, decPlaces) {
					msg := getCustomMessage(schemaField)
					(*resultMap)[fullPath] = fmt.Sprintf("%s%d decimal places", msg, decPlaces)
					continue
				}
			}
			if dateLayout := getDateFormatFromTag(schemaField); dateLayout != "" {
				goLayout := convertDateFormatToGo(dateLayout)
				if !isValidDateFormat(dataField, goLayout) {
					(*resultMap)[fullPath] = fmt.Sprintf("Invalid date format, expected %s", dateLayout)
					continue
				}
			}
			(*resultMap)[fullPath] = "valid"
		}
	}
}

func getCustomMessage(schemaField cue.Value) string {
	if attr := schemaField.Attribute("tag"); attr.Err() == nil {
		if val, found, err := attr.Lookup(0, "message"); err == nil && found {
			return strings.Trim(val, `"`)
		}
	}
	if attr := schemaField.Attribute("message"); attr.Err() == nil {
		if str, err := attr.String(0); err == nil {
			return strings.Trim(str, `"`)
		}
	}
	return ""
}

func getDecimalPlacesFromTag(schemaField cue.Value) int {
	if attr := schemaField.Attribute("tag"); attr.Err() == nil {
		if val, found, err := attr.Lookup(0, "decimal"); err == nil && found {
			if dec, err := strconv.Atoi(val); err == nil {
				return dec
			}
		}
	}
	return -1
}

func hasExactDecimalPlaces(value cue.Value, decimals int) bool {
	floatVal, err := value.Float64()
	if err != nil {
		return false
	}
	scale := math.Pow(10, float64(decimals))
	return math.Abs(floatVal*scale-math.Trunc(floatVal*scale)) < 1e-9
}

// --- DATE VALIDATION HELPERS ---

func getDateFormatFromTag(schemaField cue.Value) string {
	if attr := schemaField.Attribute("tag"); attr.Err() == nil {
		if val, found, err := attr.Lookup(0, "date"); err == nil && found {
			return strings.Trim(val, `"`)
		}
	}
	return ""
}

func convertDateFormatToGo(layout string) string {
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

func isValidDateFormat(value cue.Value, goLayout string) bool {
	strVal, err := value.String()
	if err != nil {
		return false
	}
	_, err = time.Parse(goLayout, strVal)
	return err == nil
}

func mapToCString(m map[string]string) *C.char {
	jsonBytes, _ := json.Marshal(m)
	return C.CString(string(jsonBytes))
}

func main() {}
