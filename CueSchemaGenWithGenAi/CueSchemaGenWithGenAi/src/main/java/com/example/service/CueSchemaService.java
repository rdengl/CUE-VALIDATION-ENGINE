package com.example.service;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class CueSchemaService {

    private final ChatClient chatClient;

    public CueSchemaService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String generateCueSchema(String naturalLanguageRequest) {
        String systemPrompt = """
            You are an assistant that converts natural language requirements into valid CUE schema.
            Always wrap everything inside "Request" root object.
            Note: cue dose not support the use of traditional comparison operators (e.g ==, !=, len(), maxlen(), etc) fields constraints use structural equality by assigning value directly.
            Example:
            User: "age must be number and less than 60 and greater than 18"
            Output:
            Request: {
              age: int & >18 & <60 @tag(message ="replace with validation rule message, contains ="rule value")"
            }
            and return only cue schema without any explanation and generated cue shema.
            """;

        return chatClient
                .prompt()
                .system(systemPrompt)
                .user(naturalLanguageRequest)
                .call()
                .content();
    }
}

