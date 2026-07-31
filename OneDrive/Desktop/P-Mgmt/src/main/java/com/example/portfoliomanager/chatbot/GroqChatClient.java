package com.example.portfoliomanager.chatbot;

public interface GroqChatClient {
    String chat(String systemPrompt, String userPrompt);
    String model();
}
