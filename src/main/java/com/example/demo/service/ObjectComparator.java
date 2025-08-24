package com.example.demo.service;

import java.lang.reflect.Field;
import java.util.Objects;

public class ObjectComparator {

    public static boolean isModified(Object dbObject, Object requestObject, String... fieldNames) {
        if (dbObject == null || requestObject == null) {
            return false; // or throw IllegalArgumentException
        }

        try {
            Class<?> clazz = dbObject.getClass();
            for (String fieldName : fieldNames) {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);

                Object dbValue = field.get(dbObject);
                Object reqValue = field.get(requestObject);

                // Compare values
                if (!Objects.equals(dbValue, reqValue)) {
                    return true; // field changed
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while comparing objects", e);
        }
        return false; // no changes found
    }
}
