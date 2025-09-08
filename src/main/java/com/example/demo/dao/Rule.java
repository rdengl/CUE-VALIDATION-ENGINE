package com.example.demo.dao;

import lombok.*;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Rule {
    private int id;
    private String name;
    private String status;
}
