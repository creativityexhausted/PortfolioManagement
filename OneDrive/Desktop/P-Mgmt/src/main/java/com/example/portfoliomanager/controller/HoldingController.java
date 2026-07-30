package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.HoldingRequest;
import com.example.portfoliomanager.dto.ApiDtos.HoldingResponse;
import com.example.portfoliomanager.exception.ApiError;
import com.example.portfoliomanager.service.HoldingService;
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
@RequestMapping("/api/holdings")
@Tag(name = "Holdings", description = "Endpoints for managing portfolio holdings")
public class HoldingController {

    private final HoldingService service;

    public HoldingController(HoldingService service) {
        this.service = service;
    }

    @GetMapping
        @Operation(
            summary = "List holdings",
            description = "Returns holdings and can optionally filter by portfolioId query parameter.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Holdings fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public List<HoldingResponse> findAll(
            @Parameter(description = "Optional portfolio ID filter", example = "1")
            @RequestParam(required = false) Long portfolioId) {
        return service.findAll(portfolioId);
    }

    @GetMapping("/{id}")
        @Operation(
            summary = "Get holding by ID",
            description = "Returns a specific holding by its unique ID.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Holding found"),
            @ApiResponse(responseCode = "404", description = "Holding not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public HoldingResponse findById(
            @Parameter(description = "Holding ID", example = "1", required = true)
            @PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
        @Operation(
            summary = "Create holding",
            description = "Creates a holding for a portfolio.")
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Holding created successfully"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
    public HoldingResponse create(@Valid @RequestBody HoldingRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
        @Operation(
            summary = "Update holding",
            description = "Updates an existing holding by ID.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Holding updated successfully"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Holding or portfolio not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public HoldingResponse update(
            @Parameter(description = "Holding ID", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody HoldingRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
        @Operation(
            summary = "Delete holding",
            description = "Deletes a holding by ID.")
        @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Holding deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Holding not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public void delete(
            @Parameter(description = "Holding ID", example = "1", required = true)
            @PathVariable Long id) {
        service.delete(id);
    }
}
