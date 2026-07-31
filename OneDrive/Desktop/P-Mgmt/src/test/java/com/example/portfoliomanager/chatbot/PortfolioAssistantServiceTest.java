package com.example.portfoliomanager.chatbot;

import com.example.portfoliomanager.dto.ApiDtos.ChatAssistantRequest;
import com.example.portfoliomanager.dto.ApiDtos.ChatAssistantResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortfolioAssistantServiceTest {

    private PortfolioAssistantContextService contextService;
    private GroqChatClient groqClient;
    private PortfolioAssistantService service;

    @BeforeEach
    void setUp() {
        contextService = mock(PortfolioAssistantContextService.class);
        groqClient = mock(GroqChatClient.class);
        service = new PortfolioAssistantService(contextService, groqClient);

        when(groqClient.model()).thenReturn("llama-3.1-8b-instant");
        when(contextService.build(any())).thenReturn(new PortfolioAssistantContextService.PortfolioAssistantContext(
                "ctx", "Overall performance is +100.00 (5.00%).", "all portfolios"));
    }

    @Test
    void adviceQuestionIsRefusedWithoutCallingGroq() {
        ChatAssistantResponse response = service.answer("demo", new ChatAssistantRequest("What should I buy now?", null));

        assertThat(response.answer()).contains("cannot suggest buying or selling");
        verify(groqClient, never()).chat(anyString(), anyString());
    }

    @Test
    void groqFailureReturnsHighlightsFallback() {
        when(groqClient.chat(anyString(), anyString())).thenThrow(new RuntimeException("timeout"));

        ChatAssistantResponse response = service.answer("demo", new ChatAssistantRequest("How is my portfolio doing?", null));

        assertThat(response.answer()).contains("could not reach the AI provider");
        assertThat(response.answer()).contains("Overall performance");
    }
}
