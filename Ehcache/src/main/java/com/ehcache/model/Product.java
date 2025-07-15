package com.ehcache.model;

import java.io.Serializable;

public class Product implements Serializable {
    private String id;
    private String name;

    public Product() {}

    public Product(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
}
