package com.example.portfoliomanager.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Standard error payload returned by the API")
public record ApiError(
        @Schema(description = "Error timestamp in UTC", example = "2026-07-30T12:00:00Z")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "HTTP status reason", example = "Bad Request")
        String error,
        @Schema(description = "Human-readable error message", example = "Request validation failed")
        String message,
        @Schema(description = "Validation errors keyed by request field")
        Map<String, String> validationErrors
) {
}
