package com.example.demo.utils;


import com.example.demo.dao.Rule;
import com.example.demo.dao.TemplateRuls;
import com.example.demo.service.ObjectComparator;

import java.util.*;

public class MainApp {
    public static void main(String[] args) {
        // DB list (current state in database)
        List<TemplateRuls> dbList = Arrays.asList(
                new TemplateRuls(1L, "RULE-A", "ACTIVE",Arrays.asList(new Rule(1,"rule1","success"))),
                new TemplateRuls(2L, "RULE-B", "ACTIVE",Arrays.asList(new Rule(1,"rule12","success")))
        );

        // Request list (incoming request from API/UI)
        List<TemplateRuls> reqList = Arrays.asList(
                new TemplateRuls(1L, "RULE-A", "ACTIVE",Arrays.asList(new Rule(1,"rule1","success"))),   // unchanged
                new TemplateRuls(2L, "RULE-B", "ACTIVE",Arrays.asList(new Rule(1,"rule12","success1"))),   // status modified
                new TemplateRuls(3L, "RULE-C", "ACTIVE",Arrays.asList(new Rule(1,"rule1","success")))       // new entry
        );

        // Compare lists
        Map<String, Boolean> comparisonResult = ObjectComparator.compareLists(
                dbList, reqList, "id", "name", "status","rules.name","rules.status"
        );

        // Print results
        System.out.println("Comparison Results:");
        comparisonResult.forEach((id, modified) ->
                System.out.println(id + " -> modified=" + modified)
        );
    }
}
