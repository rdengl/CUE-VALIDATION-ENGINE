package com.example.demo.service;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.*;

public class ObjectComparator {

    /**
     * Compare lists of objects (e.g., TemplateRuls) and return a map of id -> modifiedFlag.
     */
    public static <T> Map<String, Boolean> compareLists(
            List<T> dbList, List<T> reqList, String idField, String... fieldNames) {

        Map<String, Boolean> result = new LinkedHashMap<>();

        if (dbList == null) dbList = new ArrayList<>();
        if (reqList == null) reqList = new ArrayList<>();

        // Build maps by ID for easy lookup
        Map<Object, T> dbMap = buildIdMap(dbList, idField);
        Map<Object, T> reqMap = buildIdMap(reqList, idField);

        // Collect all IDs
        Set<Object> allIds = new HashSet<>();
        allIds.addAll(dbMap.keySet());
        allIds.addAll(reqMap.keySet());

        for (Object id : allIds) {
            T dbObj = dbMap.get(id);
            T reqObj = reqMap.get(id);

            // if one side missing → modified
            if (dbObj == null || reqObj == null) {
                result.put("id=" + id, true);
                continue;
            }

            boolean modified = false;
            try {
                for (String fieldPath : fieldNames) {
                    Object dbVal = getNestedFieldValue(dbObj, fieldPath);
                    Object reqVal = getNestedFieldValue(reqObj, fieldPath);
                    if (!Objects.equals(dbVal, reqVal)) {
                        modified = true;
                        break;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error comparing objects for id=" + id, e);
            }

            result.put("id=" + id, modified);
        }

        return result;
    }

    private static <T> Map<Object, T> buildIdMap(List<T> list, String idField) {
        Map<Object, T> map = new LinkedHashMap<>();
        for (T obj : list) {
            try {
                Object id = getNestedFieldValue(obj, idField);
                if (id != null) {
                    map.put(id, obj);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error reading id field=" + idField, e);
            }
        }
        return map;
    }

    /**
     * Supports nested fields like "rules.name" or "child.inner.value".
     * If a field is a List, concatenates values into a string for comparison.
     */
    private static Object getNestedFieldValue(Object obj, String fieldPath) throws Exception {
        if (obj == null) return null;

        String[] parts = fieldPath.split("\\.");
        Object current = obj;

        for (String part : parts) {
            if (current == null) return null;

            if (current instanceof List) {
                List<?> list = (List<?>) current;
                List<Object> values = new ArrayList<>();
                for (Object item : list) {
                    values.add(getNestedFieldValue(item, String.join(".", Arrays.copyOfRange(parts, Arrays.asList(parts).indexOf(part), parts.length))));
                }
                return values.toString(); // flatten to string for comparison
            }

            Field f = current.getClass().getDeclaredField(part);
            f.setAccessible(true);
            current = f.get(current);
        }
        return current;
    }
}
