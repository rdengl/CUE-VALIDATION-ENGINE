package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

public class CueSchemaGenerator {

    public static void main(String[] args) throws Exception {
        String jsonInput = """
        {
          "wirelesssConfigRule": [
            {
              "automationRules": [
                {
                  "templateAutomationRule": [
                    {
                      "type": "int",
                      "parameter": "age",
                      "oprator": "GreaterThan",
                      "value": "18"
                    },
                    {
                      "type": "int",
                      "parameter": "age",
                      "oprator": "LessThan",
                      "value": "60"
                    },
                    {
                      "type": "string",
                      "parameter": "name",
                      "oprator": "Equals",
                      "value": "John"
                    }
                  ]
                }
              ]
            }
          ]
        }
        """;

        String cueSchema = generateCueSchemaFromJson(jsonInput);
        System.out.println("Generated CUE Schema:\n" + cueSchema);
    }

    public static String generateCueSchemaFromJson(String jsonString) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonString);

        StringBuilder cueSchema = new StringBuilder();
        cueSchema.append("Request: {\n");

        // Collect all rules grouped by parameter
        Map<String, List<Rule>> paramRules = new LinkedHashMap<>();

        JsonNode ruleNodes = root.path("wirelesssConfigRule");
        for (JsonNode configRule : ruleNodes) {
            JsonNode automationRules = configRule.path("automationRules");
            for (JsonNode rule : automationRules) {
                JsonNode templateRules = rule.path("templateAutomationRule");
                for (JsonNode templateRule : templateRules) {
                    String type = templateRule.path("type").asText();
                    String parameter = templateRule.path("parameter").asText();
                    String operator = templateRule.path("oprator").asText();
                    String value = templateRule.path("value").asText();

                    if (parameter == null || parameter.isEmpty()) continue;

                    paramRules
                            .computeIfAbsent(parameter, k -> new ArrayList<>())
                            .add(new Rule(type, operator, value));
                }
            }
        }

        // Generate merged CUE rules per parameter
        for (Map.Entry<String, List<Rule>> entry : paramRules.entrySet()) {
            String param = entry.getKey();
            List<Rule> rules = entry.getValue();

            String baseType = determineBaseType(rules);
            StringBuilder conditionBuilder = new StringBuilder();
            for (Rule r : rules) {
                String condition = generateCondition(baseType, r.operator, r.value);
                if (!condition.isEmpty()) {
                    if (conditionBuilder.length() > 0) conditionBuilder.append(" & ");
                    conditionBuilder.append(condition);
                }
            }

            String tagMessage = "@tag(message=\"Validation failed for " + param + "\")";
            cueSchema.append("  ")
                    .append(param)
                    .append(": ")
                    .append(baseType)
                    .append(" & ")
                    .append(conditionBuilder)
                    .append(" ")
                    .append(tagMessage)
                    .append("\n");
        }

        cueSchema.append("}\n");
        return cueSchema.toString();
    }

    private static String generateCondition(String typePrefix, String operator, String value) {
        return switch (operator) {
            case "Equals" -> formatValue(typePrefix, value);
            case "NotEquals" -> "!= " + formatValue(typePrefix, value);
            case "GreaterThan" -> "> " + formatValue(typePrefix, value);
            case "GreaterThanOrEqual" -> ">= " + formatValue(typePrefix, value);
            case "LessThan" -> "< " + formatValue(typePrefix, value);
            case "LessThanOrEqual" -> "<= " + formatValue(typePrefix, value);
            case "Contains" -> "=~\".*" + escapeForRegex(value) + ".*\"";
            case "NotContains" -> "!~\".*" + escapeForRegex(value) + ".*\"";
            case "Regex" -> "=~\"" + escapeForRegex(value) + "\"";
            default -> "";
        };
    }

    private static String formatValue(String type, String value) {
        return "string".equalsIgnoreCase(type) ? "\"" + value + "\"" : value;
    }

    private static boolean isInteger(String val) {
        try {
            Integer.parseInt(val);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDecimal(String val) {
        try {
            new BigDecimal(val);
            return val.contains(".");
        } catch (Exception e) {
            return false;
        }
    }

    private static String escapeForRegex(String val) {
        return val.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String determineBaseType(List<Rule> rules) {
        for (Rule rule : rules) {
            if ("int".equalsIgnoreCase(rule.type) || isInteger(rule.value)) return "int";
            if ("float".equalsIgnoreCase(rule.type) || isDecimal(rule.value)) return "number";
            if ("bool".equalsIgnoreCase(rule.type)) return "bool";
        }
        return "string";
    }

    private static class Rule {
        String type;
        String operator;
        String value;

        Rule(String type, String operator, String value) {
            this.type = type;
            this.operator = operator;
            this.value = value;
        }
    }
}
