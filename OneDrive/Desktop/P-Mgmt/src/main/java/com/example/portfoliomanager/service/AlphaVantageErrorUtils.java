package com.example.portfoliomanager.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Alpha Vantage has a quirky habit of returning HTTP 200 OK even when a request is
 * rejected due to rate limiting or another error condition - it just swaps the expected
 * payload (e.g. "feed", "bestMatches", "Global Quote") for an "Information", "Note", or
 * "Error Message" field instead. Because the rest of our code deserializes into typed
 * records that ignore unknown properties, that field was previously silently dropped and
 * the request looked like it "succeeded" with zero results, hiding the real problem.
 *
 * Callers should check {@link #extractNotice(JsonNode)} on every Alpha Vantage response
 * before treating a missing/empty payload as "no data found".
 */
public final class AlphaVantageErrorUtils {

    private static final String[] NOTICE_FIELDS = {"Information", "Note", "Error Message"};

    private AlphaVantageErrorUtils() {
    }

    public static String extractNotice(JsonNode root) {
        if (root == null) {
            return null;
        }
        for (String field : NOTICE_FIELDS) {
            JsonNode node = root.get(field);
            if (node != null && !node.isNull()) {
                String text = node.asText();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    /** Convenience helper for logging: shortens the (often verbose) rate-limit message. */
    public static String summarize(String notice) {
        if (notice == null) {
            return null;
        }
        return notice.length() > 140 ? notice.substring(0, 140) + "..." : notice;
    }
}
