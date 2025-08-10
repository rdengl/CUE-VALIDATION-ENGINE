package com.example.demo.cue;

import java.util.*;

class Config {
    String rule;
    String value;

    public Config(String rule, String value) {
        this.rule = rule;
        this.value = value;
    }
}

class Parameter {
    String parameterName;
    String dataType; // String, Number, Int, Float
    String value;
    String message;
    List<Config> configurations;
    List<String> picklist; // New field

    public Parameter(String parameterName, String dataType, String value, String message,
                     List<Config> configurations, List<String> picklist) {
        this.parameterName = parameterName;
        this.dataType = dataType;
        this.value = value;
        this.message = message;
        this.configurations = configurations;
        this.picklist = picklist;
    }
}

public class CueSchemaCrateer {

    public static String generateCueSchema(List<Parameter> parameters) {
        StringBuilder schema = new StringBuilder("Request: {\n");

        for (Parameter param : parameters) {

            // Ensure exactly one of configurations or picklist is provided
            boolean hasConfigs = param.configurations != null && !param.configurations.isEmpty();
            boolean hasPicklist = param.picklist != null && !param.picklist.isEmpty();

            if (hasConfigs && hasPicklist) {
                throw new IllegalArgumentException("Parameter '" + param.parameterName +
                        "' cannot have both configurations and picklist.");
            }
            if (!hasConfigs && !hasPicklist) {
                throw new IllegalArgumentException("Parameter '" + param.parameterName +
                        "' must have either configurations or picklist.");
            }

            schema.append("\t").append(param.parameterName).append(": ");

            // Map datatype
            String cueType = mapDataType(param.dataType);
            if (param.dataType.equalsIgnoreCase("string")) {
                cueType += " & != \"\""; // Non-empty string check
            }
            schema.append(cueType);

            // If configurations exist, apply them
            if (hasConfigs) {
                String inlineConstraints = getInlineConstraints(param);
                if (!inlineConstraints.isEmpty()) {
                    schema.append(" ").append(inlineConstraints);
                }
            }

            // If picklist exists, enforce one-of constraint
            if (hasPicklist) {
                schema.append(" & (");
                for (int i = 0; i < param.picklist.size(); i++) {
                    schema.append("\"").append(param.picklist.get(i)).append("\"");
                    if (i < param.picklist.size() - 1) schema.append(" | ");
                }
                schema.append(")");
            }

            // Build @tag attributes
            StringBuilder tagBuilder = new StringBuilder("@tag(");
            if (hasConfigs) {
                for (Config cfg : param.configurations) {
                    tagBuilder.append(cfg.rule).append("=\"").append(cfg.value).append("\", ");
                }
            }
            if (hasPicklist) {
                tagBuilder.append("picklist=\"").append(String.join(",", param.picklist)).append("\", ");
            }
            if (param.dataType.equalsIgnoreCase("string")) {
                tagBuilder.append("null_or_empty_message=\"")
                        .append(param.parameterName).append(" must not be null or empty\", ");
            }
            if (param.message != null && !param.message.isEmpty()) {
                tagBuilder.append("message=\"").append(param.message).append("\", ");
            }

            // Clean trailing comma
            if (tagBuilder.toString().endsWith(", ")) {
                tagBuilder.setLength(tagBuilder.length() - 2);
            }
            tagBuilder.append(")");

            schema.append(" ").append(tagBuilder).append("\n");
        }

        schema.append("}");
        return schema.toString();
    }

    private static String mapDataType(String dataType) {
        switch (dataType.toLowerCase()) {
            case "string": return "string";
            case "number": return "number";
            case "int":
            case "integer": return "int";
            case "float":
            case "double": return "number";
            case "date": return "string & =~\"^\\\\d{4}-\\\\d{2}-\\\\d{2}$\"";
            default: return "string";
        }
    }

    private static String getInlineConstraints(Parameter param) {
        StringBuilder constraints = new StringBuilder();
        for (Config cfg : param.configurations) {
            switch (cfg.rule) {
                case "min": constraints.append("& >=").append(cfg.value).append(" "); break;
                case "max": constraints.append("& <=").append(cfg.value).append(" "); break;
                case "not_empty": constraints.append("& != \"\" "); break;
                case "min_chars": constraints.append("& len >= ").append(cfg.value).append(" "); break;
                case "max_chars": constraints.append("& len <= ").append(cfg.value).append(" "); break;
                case "must_contain": constraints.append("& =~\".*").append(cfg.value).append(".*\" "); break;
                case "must_not_contain": constraints.append("& !~\".*").append(cfg.value).append(".*\" "); break;
                case "decimal": constraints.append("& =~\"^[0-9]+(\\\\.[0-9]{").append(cfg.value).append("})?$\" "); break;
                case "decimal_max": constraints.append("& =~\"^[0-9]+(\\\\.[0-9]{1,").append(cfg.value).append("})?$\" "); break;
            }
        }
        return constraints.toString().trim();
    }

    public static void main(String[] args) {
        List<Parameter> params = new ArrayList<>();

        // With configurations
        params.add(new Parameter(
                "age", "int", "25", "Age must be between 18 and 60",
                Arrays.asList(new Config("min", "18"), new Config("max", "60")), null
        ));

        // With picklist
        params.add(new Parameter(
                "size", "string", "medium", "Size must be one of the allowed values",
                null, Arrays.asList("medium", "large", "small", "big")
        ));

        String cueSchema = generateCueSchema(params);
        System.out.println(cueSchema);
    }
}

