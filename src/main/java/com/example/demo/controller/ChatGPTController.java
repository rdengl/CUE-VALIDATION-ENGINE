package com.example.demo.controller;


import com.example.demo.service.ChatGPTService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/chat")
public class ChatGPTController {

    private final ChatGPTService chatGPTService;

    public ChatGPTController(ChatGPTService chatGPTService) {
        this.chatGPTService = chatGPTService;
    }

    @PostMapping
    public String chat(@RequestBody String userMessage) throws IOException {
        return chatGPTService.askChatGPT(userMessage);
    }
}
