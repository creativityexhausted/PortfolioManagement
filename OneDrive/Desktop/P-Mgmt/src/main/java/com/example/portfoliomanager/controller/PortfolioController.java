package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.PortfolioRequest;
import com.example.portfoliomanager.dto.ApiDtos.PortfolioResponse;
import com.example.portfoliomanager.exception.ApiError;
import com.example.portfoliomanager.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@Tag(name = "Portfolios", description = "Endpoints for managing user portfolios")
public class PortfolioController {

    private final PortfolioService service;

    public PortfolioController(PortfolioService service) {
        this.service = service;
    }

    @GetMapping
        @Operation(
            summary = "List portfolios",
            description = "Returns all portfolios available to the authenticated user.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolios fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
    public List<PortfolioResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
        @Operation(
            summary = "Get portfolio by ID",
            description = "Returns one portfolio by its unique identifier.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolio found"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public PortfolioResponse findById(
            @Parameter(description = "Portfolio ID", example = "1", required = true)
            @PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
        @Operation(
            summary = "Create portfolio",
            description = "Creates a new portfolio using the provided request body.")
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Portfolio created successfully"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
    public PortfolioResponse create(@Valid @RequestBody PortfolioRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
        @Operation(
            summary = "Update portfolio",
            description = "Updates an existing portfolio by ID using the supplied request body.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Portfolio updated successfully"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public PortfolioResponse update(
            @Parameter(description = "Portfolio ID", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody PortfolioRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
        @Operation(
            summary = "Delete portfolio",
            description = "Deletes a portfolio by ID.")
        @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Portfolio deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public void delete(
            @Parameter(description = "Portfolio ID", example = "1", required = true)
            @PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/optimize")
    @Operation(
            summary = "Quantum optimize portfolio",
            description = "Optimizes a portfolio using QAOA quantum algorithm.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Optimization successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public com.example.portfoliomanager.dto.ApiDtos.QuantOptimizationResponse optimize(
            @Parameter(description = "Portfolio ID", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody com.example.portfoliomanager.dto.ApiDtos.QuantOptimizationRequest request) {
        return service.optimizePortfolio(id, request);
    }
}
