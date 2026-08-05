package com.example.fundamentals.dto;

import java.util.List;
import java.util.Map;

public final class SnowflakeDtos {

    private SnowflakeDtos() {
    }

    /**
     * One axis of the Snowflake radar chart. Score is 0-6 (matches Simply Wall St's scale),
     * with a human-readable explanation of how it was derived and whether data was missing.
     */
    public record AxisScore(
            String axis,          // "Value" | "Future" | "Past" | "Health" | "Dividend"
            int score,            // 0-6
            String explanation,   // e.g. "P/E of 18.4 is below the 25 benchmark -> strong value score"
            boolean estimated     // true if a neutral/default score was used due to missing data
    ) {
    }

    /** Full Snowflake result for one symbol, ready for the frontend RadarChart. */
    public record SnowflakeScore(
            String symbol,
            String companyName,
            AxisScore value,
            AxisScore future,
            AxisScore past,
            AxisScore health,
            AxisScore dividend,
            double overallScore,      // average of the 5 axis scores, 0-6
            String overallLabel,      // "Excellent" | "Good" | "Average" | "Weak" | "Poor"
            Map<String, Object> rawMetrics, // the underlying Finnhub metrics used, for transparency
            List<String> notes        // e.g. "Dividend data unavailable for this symbol; assumed neutral score"
    ) {
    }
}
