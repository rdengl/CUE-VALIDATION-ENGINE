package com.example.demo.cue;

import java.util.*;
import java.util.regex.*;

public class ExtractingValuesFromCue {

    public static Map<String, String> extractCueFields(String cueInput) {
        Map<String, String> fieldMap = new LinkedHashMap<>();

        // Remove the "Request: {" and closing "}"
        String trimmed = cueInput.replaceAll("(?s)^Request:\\s*\\{\\s*", "").replaceAll("\\}\\s*$", "");

        // Regex to match: key: valueExpression (even with @tag inside)
        Pattern pattern = Pattern.compile("(?m)^\\s*([a-zA-Z0-9_]+):\\s*(.+?)(?=^\\s*[a-zA-Z0-9_]+:|\\Z)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(trimmed);

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim().replaceAll("\n", " ");
            fieldMap.put(key, value);
        }

        return fieldMap;
    }

    public static void main(String[] args) {
        String cue = """
        Request: {
          parentLevelDecimal: number & >= 0.00 @tag(message="Validation failed for parentLevelDecimal", decimal="2")
          roles1: [...string] @tag(not_contains="[guest]", message="Validation failed for roles1")
          roles2: [...string] @tag(contains_any="[manager,supervisor]", message="Validation failed for roles2")
        }
        """;

        Map<String, String> result = extractCueFields(cue);
        result.forEach((k, v) -> System.out.println(k + " => " + v));
    }
}

