package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.ChatAssistantRequest;
import com.example.portfoliomanager.dto.ApiDtos.ChatAssistantResponse;
import com.example.portfoliomanager.service.PortfolioAssistantFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioAssistantControllerTest {

    @Mock
    private PortfolioAssistantFacade assistantFacade;

    @InjectMocks
    private PortfolioAssistantController controller;

    @Test
    void askDelegatesToFacadeAndReturnsResponse() {
        ChatAssistantRequest request = new ChatAssistantRequest("Summarize my top risks", 1L);
        ChatAssistantResponse expected = new ChatAssistantResponse(
                "Top concentration risk is in tech holdings.",
                "llama-3.3-70b-versatile",
                LocalDateTime.now());
        when(assistantFacade.answer(request)).thenReturn(expected);

        ChatAssistantResponse actual = controller.ask(request);

        assertThat(actual).isEqualTo(expected);
        verify(assistantFacade).answer(request);
    }
}
