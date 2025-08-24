/*
package com.example.demo.cue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public Parameter(String parameterName, String dataType, String value, String message, List<Config> configurations) {
        this.parameterName = parameterName;
        this.dataType = dataType;
        this.value = value;
        this.message = message;
        this.configurations = configurations;
    }
}

public class CueSchmaGeneratorForOprators {

    public static String generateCueSchema(List<Parameter> parameters) {
        StringBuilder schema = new StringBuilder("Request: {\n");

        for (Parameter param : parameters) {
            schema.append("\t").append(param.parameterName).append(": ");

            // Map Java datatype to CUE type
            String cueType = mapDataType(param.dataType);

            // For Strings: always add not null and not empty check
            if (param.dataType.equalsIgnoreCase("string")) {
                cueType += " & != \"\""; // Non-empty string check
            }

            schema.append(cueType);

            // Add inline constraints if applicable
            String inlineConstraints = getInlineConstraints(param);
            if (!inlineConstraints.isEmpty()) {
                schema.append(" ").append(inlineConstraints);
            }

            // Build @tag attributes
            StringBuilder tagBuilder = new StringBuilder("@tag(");
            for (Config cfg : param.configurations) {
                tagBuilder.append(cfg.rule).append("=\"").append(cfg.value).append("\", ");
            }

            // Add default null/empty error message for strings
            if (param.dataType.equalsIgnoreCase("string")) {
                tagBuilder.append("null_or_empty_message=\"")
                        .append(param.parameterName).append(" must not be null or empty\", ");
            }

            if (param.message != null && !param.message.isEmpty()) {
                tagBuilder.append("message=\"").append(param.message).append("\", ");
            }

            // Remove trailing comma and space
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
            case "string":
                return "string";
            case "number":
                return "number";
            case "int":
            case "integer":
                return "int";
            case "float":
            case "double":
                return "number";
            case "date":
                return "string & =~\"^\\\\d{4}-\\\\d{2}-\\\\d{2}$\"";
            default:
                return "string";
        }
    }

    private static String getInlineConstraints(Parameter param) {
        StringBuilder constraints = new StringBuilder();

        for (Config cfg : param.configurations) {
            switch (cfg.rule) {
                case "min":
                    constraints.append("& >=").append(cfg.value).append(" ");
                    break;
                case "max":
                    constraints.append("& <=").append(cfg.value).append(" ");
                    break;
                case "not_empty":
                    constraints.append("& != \"\" ");
                    break;
                case "min_chars":
                    constraints.append("& len >= ").append(cfg.value).append(" ");
                    break;
                case "max_chars":
                    constraints.append("& len <= ").append(cfg.value).append(" ");
                    break;
                case "must_contain":
                    constraints.append("& =~\".*").append(cfg.value).append(".*\" ");
                    break;
                case "must_not_contain":
                    constraints.append("& !~\".*").append(cfg.value).append(".*\" ");
                    break;
                case "decimal":
                    constraints.append("& =~\"^[0-9]+(\\\\.[0-9]{").append(cfg.value).append("})?$\" ");
                    break;
                case "decimal_max":
                    constraints.append("& =~\"^[0-9]+(\\\\.[0-9]{1,").append(cfg.value).append("})?$\" ");
                    break;
            }
        }
        return constraints.toString().trim();
    }

    public static void main(String[] args) {
        List<Parameter> params = new ArrayList<>();

        params.add(new Parameter(
                "age",
                "int",
                "25",
                "Age must be between 18 and 60",
                Arrays.asList(
                        new Config("min", "18"),
                        new Config("max", "60"),
                        new Config("min_chars", "1")
                )
        ));

        params.add(new Parameter(
                "status",
                "string",
                "activeUser",
                "Status must contain 'active' and be 6–20 chars long",
                Arrays.asList(
                        new Config("must_contain", "active"),
                        new Config("min_chars", "6"),
                        new Config("max_chars", "20")
                )
        ));

        params.add(new Parameter(
                "childLevelDecimal",
                "number",
                "12.34",
                "Must have exactly 2 decimal places",
                Arrays.asList(
                        new Config("decimal", "2")
                )
        ));

        params.add(new Parameter(
                "price",
                "number",
                "100.123",
                "Must have at most 3 decimal places",
                Arrays.asList(
                        new Config("decimal_max", "3")
                )
        ));

        params.add(new Parameter(
                "name",
                "string",
                "Bob",
                "Name must not contain 'Alice'",
                Arrays.asList(
                        new Config("must_not_contain", "Alice")
                )
        ));

        params.add(new Parameter(
                "salary",
                "int",
                "45000",
                "Salary must be less than 6 characters",
                Arrays.asList(
                        new Config("max_chars", "6")
                )
        ));

        params.add(new Parameter(
                "description",
                "string",
                "A detailed job description",
                "Description must be between 10 and 50 characters",
                Arrays.asList(
                        new Config("min_chars", "10"),
                        new Config("max_chars", "50")
                )
        ));

        String cueSchema = generateCueSchema(params);
        System.out.println(cueSchema);
    }
}
*/
