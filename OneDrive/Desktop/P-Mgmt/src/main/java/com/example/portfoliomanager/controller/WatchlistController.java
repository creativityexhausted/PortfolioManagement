package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.WatchlistRequest;
import com.example.portfoliomanager.dto.ApiDtos.WatchlistResponse;
import com.example.portfoliomanager.exception.ApiError;
import com.example.portfoliomanager.service.WatchlistService;
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
@RequestMapping("/api/watchlist")
@Tag(name = "Watchlist", description = "Endpoints for managing watchlist entries")
public class WatchlistController {

    private final WatchlistService service;

    public WatchlistController(WatchlistService service) {
        this.service = service;
    }

    @GetMapping
        @Operation(
            summary = "List watchlist entries",
            description = "Returns watchlist entries and can optionally filter by portfolioId.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Watchlist entries fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public List<WatchlistResponse> findAll(
            @Parameter(description = "Optional portfolio ID filter", example = "1")
            @RequestParam(required = false) Long portfolioId) {
        return service.findAll(portfolioId);
    }

    @GetMapping("/{id}")
        @Operation(
            summary = "Get watchlist entry by ID",
            description = "Returns a specific watchlist entry by ID.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Watchlist entry found"),
            @ApiResponse(responseCode = "404", description = "Watchlist entry not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public WatchlistResponse findById(
            @Parameter(description = "Watchlist entry ID", example = "1", required = true)
            @PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
        @Operation(
            summary = "Create watchlist entry",
            description = "Creates a watchlist entry under a portfolio.")
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Watchlist entry created successfully"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Portfolio not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
    public WatchlistResponse create(@Valid @RequestBody WatchlistRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
        @Operation(
            summary = "Update watchlist entry",
            description = "Updates an existing watchlist entry by ID.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Watchlist entry updated successfully"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Watchlist entry or portfolio not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public WatchlistResponse update(
            @Parameter(description = "Watchlist entry ID", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody WatchlistRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
        @Operation(
            summary = "Delete watchlist entry",
            description = "Deletes a watchlist entry by ID.")
        @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Watchlist entry deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Watchlist entry not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public void delete(
            @Parameter(description = "Watchlist entry ID", example = "1", required = true)
            @PathVariable Long id) {
        service.delete(id);
    }
}
