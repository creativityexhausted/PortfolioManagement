package com.example.portfoliomanager.chatbot;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.portfoliomanager.exception.ExternalApiException;

@Component
public class GroqApiChatClient implements GroqChatClient {

    private final RestClient restClient;
    private final GroqProperties properties;

    public GroqApiChatClient(GroqProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getGroq().getBaseUrl())
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, properties.getGroq().getModel());
    }

    @Override
    public String chat(String systemPrompt, String userPrompt, String modelOverride) {
        String apiKey = properties.getGroq().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY is not configured");
        }

        String model = (modelOverride == null || modelOverride.isBlank())
                ? properties.getGroq().getModel()
                : modelOverride;

        GroqChatResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(new GroqChatRequest(
                        model,
                        List.of(
                                new GroqMessage("system", systemPrompt),
                                new GroqMessage("user", userPrompt)
                        ),
                        properties.getGroq().getTemperature()))
                .retrieve()
                .body(GroqChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().get(0).message() == null
                || response.choices().get(0).message().content() == null
                || response.choices().get(0).message().content().isBlank()) {
            throw new ExternalApiException("Groq returned an empty response", null);
        }

        return response.choices().get(0).message().content().trim();
    }

    @Override
    public String model() {
        return properties.getGroq().getModel();
    }

    private record GroqChatRequest(String model, List<GroqMessage> messages, double temperature) {
    }

    private record GroqMessage(String role, String content) {
    }

    private record GroqChatResponse(List<GroqChoice> choices) {
    }

    private record GroqChoice(GroqMessage message) {
    }
}
