package com.example.demo;

import java.util.*;


public class HandleAllCases {

    static class CueNode {
        String name;
        boolean isArray = false;
        boolean isMap = false;
        String mapKey = "string";
        Map<String, CueNode> children = new LinkedHashMap<>();
        String type;
        List<String> validations = new ArrayList<>();
        String message;

        CueNode(String name) {
            this.name = name;
        }

        boolean isLeaf() {
            return type != null;
        }
    }

    public static String generateCueSchema(List<String> rules) {
        CueNode root = new CueNode("Request");

        for (String rule : rules) {
            parseRule(root, rule);
        }

        StringBuilder sb = new StringBuilder();
        buildCueSchema(sb, root, 0);
        return "Request: "+sb.toString()+"";
    }

    private static void parseRule(CueNode root, String rule) {
        String[] mainParts = rule.split("->");
        String[] pathType = mainParts[0].split(":");
        String fieldPath = pathType[0];
        String type = pathType.length > 1 ? pathType[1] : "string";

        List<String> validations = new ArrayList<>();
        String message = null;

        for (int i = 1; i < mainParts.length; i++) {
            switch (mainParts[i]) {
                case ">=": case "<=": case ">": case "<":
                    validations.add(mainParts[i] + mainParts[++i]);
                    break;
                case "regex":
                    validations.add("=~\"" + mainParts[++i] + "\"");
                    break;
                case "msg":
                    message = mainParts[++i];
                    break;
            }
        }

        String[] parts = fieldPath.split("\\.");
        CueNode current = root;
        for (String part : parts) {


            boolean isArray = part.contains("[]");
            boolean isMap = part.contains("[string]");
            //boolean isArray = Arrays.stream(parts).anyMatch(p -> p.contains("[]"));
            //boolean isMap = Arrays.stream(parts).anyMatch(p -> p.contains("[string]"));
            if (isArray) current.isArray = true;
            if (isMap) current.isMap = true;

            if (part.equals("[]")) continue;
            if (part.equals("[string]")) continue;

            String cleanPart = part.replace("[]", "").replace("[string]", "");

            current.children.putIfAbsent(cleanPart, new CueNode(cleanPart));
            current = current.children.get(cleanPart);


        }

        current.type = type;
        current.validations = validations;
        current.message = message;
    }

    private static void buildCueSchema(StringBuilder sb, CueNode node, int indent) {
        String indentStr = "  ".repeat(indent);

        if (!node.name.equals("Request")) {

            sb.append(indentStr).append(node.name).append(": ");
        }

        if (node.isMap && !node.children.isEmpty()) {
            sb.append("[string]: ");
        }

        //if (node.isArray) {
            if(node.isArray && !node.children.isEmpty()){
            sb.append("[...");
            indent++;
        }

        if (!node.children.isEmpty()) {
            sb.append("{\n");
            for (CueNode child : node.children.values()) {
                buildCueSchema(sb, child, indent + 1);
            }
            sb.append("  ".repeat(indent)).append("}");
        } else if (node.type != null) {
            sb.append(node.type);
            for (String v : node.validations) {
                sb.append(" & ").append(v);
            }
            if (node.message != null) {
                sb.append(" @tag(\"").append(node.message).append("\")");
            }
        }

        if(node.isArray && !node.children.isEmpty()){
            sb.append("\n").append("  ".repeat(indent - 1)).append("]");
        }

        sb.append("\n");
    }

    public static void main(String[] args) {
        List<String> rules = List.of(

                //"parenttype:string",
                "personList.[].name:string"
                //"personList.[].age:int->>=->18"
               // "personList.[].address[].pin :int->>=->18"
        );

        List<String> rules2 = List.of(

                "person.address.zip:int->>=->100000-><=->999999",
                "personList.[].name:string",
                "personList.[].age:int->>=->18",
                "attributes.[string].value:number->>=->0",
                "meta.tags.[].label:string->msg->Label required"
        );

        List<String> rules1 = List.of(
                "parenttype:string",
                "person.name:string",
                "person.age:int->>=->18",
                "person.address.city:string",
                "person.address.zip:int->>=->100000-><=->999999",
                "personList.[].name:string",
                "personList.[].age:int->>=->18",
                "attributes.[string].value:number->>=->0",
                "meta.tags.[].label:string->msg->Label required"
        );

        String schema = generateCueSchema(rules1);
        System.out.println(schema);
    }
}
