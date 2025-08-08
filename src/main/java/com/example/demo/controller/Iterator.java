package com.example.demo.controller;

public class Iterator {
    public static String buildRuleString(List<String> rules) {
        String joined = String.join(" ", rules).trim();

        // Convert to lowercase for safe comparison
        String lower = joined.toLowerCase();

        if (lower.endsWith(" and")) {
            return joined.substring(0, joined.lastIndexOf(" and")).trim();
        } else if (lower.endsWith(" or")) {
            return joined.substring(0, joined.lastIndexOf(" or")).trim();
        } else {
            return joined;
        }
    }
}
