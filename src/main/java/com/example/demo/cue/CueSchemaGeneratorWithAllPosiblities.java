package com.example.demo.cue;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class CueSchemaGeneratorWithAllPosiblities {

    public static void main(String[] args) throws Exception {
		/*
		 * String jsonInput = """ { "person": { "name": "Alice", "details": { "age": 30,
		 * "dob": "1990-01-01", "extra": "ignoreThis" }, "nonCueField": "ignore" },
		 * "metadata": { "envs": { "dev": "v1.0", "prod": "v2.0" } }, "salary": 5000 }
		 * """;
		 * 
		 * // Only include fields that exist in this map Map<String, String>
		 * cueConstraints = Map.ofEntries( Map.entry("name",
		 * "string & != \"\" @tag(message=\"Name must not be empty\")"),
		 * Map.entry("age",
		 * "int & >= 18 & <= 60 @tag(message=\"Age must be between 18 and 60\")"),
		 * Map.entry("dob",
		 * "string & =~\"^\\\\d{4}-\\\\d{2}-\\\\d{2}$\" @tag(message=\"DOB must be in YYYY-MM-DD format\")"
		 * ) // "salary", "envs", etc. are intentionally missing to show filtering );
		 */

        String jsonInput = """
                {
                    "person": {
                        "name": "Alice",
                        "details": {
                            "age": 30,
                            "dob": "1994-01-01",
                            "tags": ["engineer", "developer"]
                        },
                        "addresses": [
                            { "city": "Mumbai", "zip": 400001 },
                            { "city": "Delhi", "zip": 110001 }
                        ]
                    },
                    "salary": 5000,
                    "metadata": {
                        "envs": {
                            "dev": "v1.0",
                            "prod": "v2.0"
                        }
                    }
                }
                """;
        
     // Flat cue schema constraints
        Map<String, String> cueConstraints = Map.ofEntries(
           // Map.entry("name", "string & != \"\" @tag(message=\"Name must not be empty\")"),
            Map.entry("age", "int & >= 18 & <= 60 @tag(message=\"Age must be between 18 and 60\")"),
            Map.entry("dob", "string & =~\"^\\\\d{4}-\\\\d{2}-\\\\d{2}$\" @tag(message=\"DOB must be in YYYY-MM-DD format\")"),
            Map.entry("tags", "[...string] @tag(message=\"Tags must be a list of strings\")"),
            Map.entry("city", "string"),
            Map.entry("zip", "int & > 0")
           // Map.entry("salary", "int @tag(message=\"Salary must be an integer and not null\")"),
           // Map.entry("envs", "{[string]: string}"), // map-style constraint
           // Map.entry("dev", "int & > 0")
        );
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonTree = mapper.readTree(jsonInput);

        StringBuilder cueBuilder = new StringBuilder("Request: {\n");
        buildCueFiltered(jsonTree, cueConstraints, cueBuilder, "  ");
        cueBuilder.append("}\n");

        System.out.println(cueBuilder.toString());
    }

    // Recursive method: build schema only with fields in cueConstraints
    private static void buildCueFiltered(JsonNode node, Map<String, String> cueMap, StringBuilder cue, String indent) {
        if (!node.isObject()) return;

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode child = entry.getValue();

            // Case: Primitive value (string, int, etc.)
            if (child.isValueNode()) {
                if (cueMap.containsKey(key)) {
                    cue.append(indent).append(key).append(": ").append(cueMap.get(key)).append("\n");
                }
            }

            // Case: Array (check if it should be included)
            else if (child.isArray()) {
                if (child.size() > 0) {
                    JsonNode first = child.get(0);
                    if (first.isObject()) {
                        StringBuilder arrayBody = new StringBuilder();
                        buildCueFiltered(first, cueMap, arrayBody, indent + "    ");
                        if (arrayBody.length() > 0) {
                            cue.append(indent).append(key).append(": [...\n");
                            cue.append(indent).append("  {\n");
                            cue.append(arrayBody);
                            cue.append(indent).append("  }\n");
                            cue.append(indent).append("]\n");
                        }
                    } else if (first.isValueNode() && cueMap.containsKey(key)) {
                        cue.append(indent).append(key).append(": ").append(cueMap.get(key)).append("\n");
                    }
                }
            }

            // Case: Object (only include if any child matches constraints)
            else if (child.isObject()) {
                StringBuilder nested = new StringBuilder();
                buildCueFiltered(child, cueMap, nested, indent + "  ");
                if (nested.length() > 0) {
                    cue.append(indent).append(key).append(": {\n");
                    cue.append(nested);
                    cue.append(indent).append("}\n");
                }
            }
        }
    }
}
