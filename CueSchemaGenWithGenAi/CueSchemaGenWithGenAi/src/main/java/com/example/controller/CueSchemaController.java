package com.example.controller;


import com.example.service.CueSchemaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cue-schema")
public class CueSchemaController {

    private final CueSchemaService cueSchemaService;

    public CueSchemaController(CueSchemaService cueSchemaService) {
        this.cueSchemaService = cueSchemaService;
    }

    @PostMapping
    public String generate(@RequestBody String naturalLanguageRequest) {
        return cueSchemaService.generateCueSchema(naturalLanguageRequest);
    }
}
