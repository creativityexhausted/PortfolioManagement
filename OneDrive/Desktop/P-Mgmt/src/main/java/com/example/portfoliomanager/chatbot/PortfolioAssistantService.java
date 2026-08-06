package com.example.portfoliomanager.chatbot;

import com.example.portfoliomanager.dto.ApiDtos.ChatAssistantRequest;
import com.example.portfoliomanager.dto.ApiDtos.ChatAssistantResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class PortfolioAssistantService {

    private final PortfolioAssistantContextService contextService;
    private final GroqChatClient groqChatClient;

    public PortfolioAssistantService(
            PortfolioAssistantContextService contextService,
            GroqChatClient groqChatClient) {
        this.contextService = contextService;
        this.groqChatClient = groqChatClient;
    }

    public ChatAssistantResponse answer(String username, ChatAssistantRequest request) {
        PortfolioAssistantContextService.PortfolioAssistantContext context =
                contextService.build(request.portfolioId());

        String answer;
        if (isAdviceRequest(request.message())) {
            answer = "I can help explain your portfolio performance, but I cannot suggest buying or selling specific stocks. "
                    + context.highlights();
        } else {
            answer = askGroq(username, request.message(), context);
        }

        return new ChatAssistantResponse(answer, groqChatClient.model(), LocalDateTime.now());
    }

    private String askGroq(
            String username,
            String userMessage,
            PortfolioAssistantContextService.PortfolioAssistantContext context) {
        String systemPrompt = """
                You are TARS, a portfolio assistant.
                Rules:
                1) Use only the provided portfolio context data.
                2) Explain in simple, beginner-friendly language.
                3) Never provide investment recommendations or instructions to buy/sell/hold a specific stock.
                4) If asked for advice, refuse politely and offer factual performance observations.
                5) Be concise and structured.
                """;

        String userPrompt = """
                Username: %s

                Portfolio context:
                %s

                User question:
                %s
                """.formatted(username, context.llmContext(), userMessage);

        try {
            return groqChatClient.chat(systemPrompt, userPrompt);
        } catch (RuntimeException ex) {
            return "I could not reach the AI provider right now. " + context.highlights();
        }
    }

    private boolean isAdviceRequest(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("what should i buy")
                || lower.contains("what should i sell")
                || lower.contains("should i buy")
                || lower.contains("should i sell")
                || lower.contains("recommend")
                || lower.contains("pick a stock")
                || lower.contains("best stock to buy");
    }
}
