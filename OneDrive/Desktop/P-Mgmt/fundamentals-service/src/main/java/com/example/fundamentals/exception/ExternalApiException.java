package com.example.fundamentals.exception;

/** Thrown whenever Finnhub (or any upstream fundamentals provider) cannot supply usable data. */
public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
