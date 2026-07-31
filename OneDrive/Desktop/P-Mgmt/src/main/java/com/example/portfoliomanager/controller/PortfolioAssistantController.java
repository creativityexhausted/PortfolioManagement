package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.ChatAssistantRequest;
import com.example.portfoliomanager.dto.ApiDtos.ChatAssistantResponse;
import com.example.portfoliomanager.exception.ApiError;
import com.example.portfoliomanager.service.PortfolioAssistantFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Portfolio Assistant", description = "Chat endpoint for portfolio explanations")
public class PortfolioAssistantController {

    private final PortfolioAssistantFacade assistantFacade;

    public PortfolioAssistantController(PortfolioAssistantFacade assistantFacade) {
        this.assistantFacade = assistantFacade;
    }

    @PostMapping("/portfolio-assistant")
    @Operation(
            summary = "Ask portfolio assistant",
            description = "Answers portfolio questions in simple language without giving buy/sell recommendations.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assistant response generated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ChatAssistantResponse ask(@Valid @RequestBody ChatAssistantRequest request) {
        return assistantFacade.answer(request);
    }
}
