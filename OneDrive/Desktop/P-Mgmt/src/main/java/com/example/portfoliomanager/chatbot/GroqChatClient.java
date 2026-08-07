package com.example.portfoliomanager.chatbot;

public interface GroqChatClient {
    String chat(String systemPrompt, String userPrompt);

    /**
     * Same as {@link #chat(String, String)} but allows overriding the model used for this
     * particular call (e.g. using a stronger model for news analysis vs. the default chat model).
     */
    default String chat(String systemPrompt, String userPrompt, String modelOverride) {
        return chat(systemPrompt, userPrompt);
    }

    String model();
}
