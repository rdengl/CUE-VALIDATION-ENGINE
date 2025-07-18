package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.*;

public class CueSchemaGenerator {

    private static final String DEFAULT_DECIMAL_PRECISION = "2";

    public static void main(String[] args) throws Exception {
        String jsonInput = """
        {
          "wirelesssConfigRule": [
            {
              "automationRules": [
                {
                  "templateAutomationRule": [
                    {
                      "type": "number",
                      "parameter": "parentLevelDecimal",
                      "oprator": "GreaterThanOrEqual",
                      "value": "0.00"
                    },
                    {
                      "type": "number",
                      "parameter": "parentLevelDecimal",
                      "oprator": "LessThanOrEqual",
                      "value": "9999999999.99"
                    },
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
                    },
                    {
                      "type": "list",
                      "parameter": "roles",
                      "oprator": "required_all",
                      "value": "admin,user"
                    },
                    {
                      "type": "list",
                      "parameter": "roles1",
                      "oprator": "not_contains",
                      "value": "guest"
                    },
                    {
                      "type": "list",
                      "parameter": "roles2",
                      "oprator": "contains_any",
                      "value": "manager,supervisor"
                    }
                  ]
                }
              ]
            }
          ]
        }
        """;

        System.out.println("From parentAttr:");
        System.out.println(generateCueSchemaFromJson(jsonInput, "parentAttr"));
        System.out.println("\nFrom childWithArr:");
        System.out.println(generateCueSchemaFromJson(jsonInput, "childWithArr"));
    }

    public static String generateCueSchemaFromJson(String jsonString, String callerType) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonString);

        StringBuilder cueSchema = new StringBuilder();
        cueSchema.append("Request: {\n");

        Map<String, List<Rule>> paramRules = new LinkedHashMap<>();
        Map<String, Map<String, List<String>>> listTags = new LinkedHashMap<>();

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

                    if ("list".equalsIgnoreCase(type) && Set.of("required_all", "contains_any", "not_contains").contains(operator)) {
                        listTags.computeIfAbsent(parameter, k -> new LinkedHashMap<>())
                                .computeIfAbsent(operator, k -> new ArrayList<>())
                                .addAll(Arrays.asList(value.split(",")));
                    } else {
                        paramRules.computeIfAbsent(parameter, k -> new ArrayList<>())
                                .add(new Rule(type, operator, value));
                    }
                }
            }
        }

        String fieldBlock = generateFieldBlocks(paramRules, listTags, callerType.equals("childWithArr") ? "    " : "  ");

        if (callerType.equals("childWithArr")) {
            cueSchema.append("  rawData: [...{\n").append(fieldBlock).append("  }]\n");
        } else {
            cueSchema.append(fieldBlock);
        }

        cueSchema.append("}\n");
        return cueSchema.toString();
    }

    private static String generateFieldBlocks(
            Map<String, List<Rule>> paramRules,
            Map<String, Map<String, List<String>>> listTags,
            String indent
    ) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<Rule>> entry : paramRules.entrySet()) {
            String param = entry.getKey();
            List<Rule> rules = entry.getValue();

            String baseType = determineBaseType(rules);
            StringBuilder conditionBuilder = new StringBuilder();

            boolean isDecimal = false;
            for (Rule r : rules) {
                if (isDecimal(r.value)) isDecimal = true;
                String condition = generateCondition(baseType, r.operator, r.value);
                if (!condition.isEmpty()) {
                    if (conditionBuilder.length() > 0) conditionBuilder.append(" & ");
                    conditionBuilder.append(condition);
                }
            }

            StringBuilder tag = new StringBuilder("@tag(message=\"Validation failed for " + param + "\"");
            if ("number".equalsIgnoreCase(baseType) && isDecimal) {
                tag.append(", decimal=\"").append(DEFAULT_DECIMAL_PRECISION).append("\"");
            }
            tag.append(")");

            sb.append(indent).append(param).append(": ").append(baseType).append(" & ")
                    .append(conditionBuilder).append(" ").append(tag).append("\n");
        }

        for (Map.Entry<String, Map<String, List<String>>> entry : listTags.entrySet()) {
            String param = entry.getKey();
            Map<String, List<String>> tagMap = entry.getValue();

            StringBuilder tagBuilder = new StringBuilder("@tag(");
            for (Map.Entry<String, List<String>> t : tagMap.entrySet()) {
                tagBuilder.append(t.getKey()).append("=\"[").append(String.join(",", t.getValue())).append("]\", ");
            }
            tagBuilder.append("message=\"Validation failed for ").append(param).append("\")");

            sb.append(indent).append(param).append(": [...string] ").append(tagBuilder).append("\n");
        }

        return sb.toString();
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
            if ("float".equalsIgnoreCase(rule.type) || "number".equalsIgnoreCase(rule.type) || isDecimal(rule.value)) return "number";
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
