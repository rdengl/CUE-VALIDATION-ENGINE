package com.example.demo.dao;


import java.util.List;

public class TemplateRuls {
    private Long id;
    private String name;
    private String status;
    private List<Rule> rules;

    public TemplateRuls(Long id, String name, String status,List<Rule> rules) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.rules = rules;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }


    @Override
    public String toString() {
        return "TemplateRuls{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
