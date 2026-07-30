package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.TransactionRequest;
import com.example.portfoliomanager.dto.ApiDtos.TransactionResponse;
import com.example.portfoliomanager.exception.ApiError;
import com.example.portfoliomanager.service.TransactionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Endpoints for managing portfolio transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping
        @Operation(
            summary = "List transactions",
            description = "Returns transactions and can optionally filter by portfolioId.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public List<TransactionResponse> findAll(
            @Parameter(description = "Optional portfolio ID filter", example = "1")
            @RequestParam(required = false) Long portfolioId) {
        return service.findAll(portfolioId);
    }

    @GetMapping("/{id}")
        @Operation(
            summary = "Get transaction by ID",
            description = "Returns a specific transaction by ID.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public TransactionResponse findById(
            @Parameter(description = "Transaction ID", example = "1", required = true)
            @PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
        @Operation(
            summary = "Create transaction",
            description = "Creates a new transaction under a portfolio.")
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
        @Operation(
            summary = "Update transaction",
            description = "Updates an existing transaction by ID.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction updated successfully"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Transaction or portfolio not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public TransactionResponse update(
            @Parameter(description = "Transaction ID", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
        @Operation(
            summary = "Delete transaction",
            description = "Deletes a transaction by ID.")
        @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public void delete(
            @Parameter(description = "Transaction ID", example = "1", required = true)
            @PathVariable Long id) {
        service.delete(id);
    }
}
