package com.example.portfoliomanager.service;

import com.example.portfoliomanager.chatbot.PortfolioAssistantService;
import com.example.portfoliomanager.dto.ApiDtos.ChatAssistantRequest;
import com.example.portfoliomanager.dto.ApiDtos.ChatAssistantResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PortfolioAssistantFacade {

    private final PortfolioAssistantService assistantService;

    public PortfolioAssistantFacade(PortfolioAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    public ChatAssistantResponse answer(ChatAssistantRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication == null ? "anonymous" : authentication.getName();
        return assistantService.answer(username, request);
    }
}
