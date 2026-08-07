package com.example.fundamentals.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.fundamentals.dto.SnowflakeDtos.AxisScore;

/**
 * Converts raw Finnhub fundamental metrics into 5 axis scores (0-6 each), mirroring the
 * Value / Future / Past / Health / Dividend axes of Simply Wall St's "Snowflake" chart.
 *
 * IMPORTANT: Simply Wall St's exact formula is proprietary and undisclosed. This scoring
 * rubric is our OWN, transparent, documented approximation using publicly available
 * Finnhub fundamental ratios. Every threshold below is a deliberate, explainable design
 * choice - not a black box - so it can be defended and adjusted.
 *
 * Scoring philosophy: each axis starts at a neutral 3/6. Each supporting metric nudges the
 * score up or down within documented bands. If a required metric is missing from Finnhub's
 * response (common for smaller-cap or non-US symbols), the axis falls back to the neutral
 * score of 3 and is flagged as "estimated" so the frontend/user knows it's not fully backed
 * by data.
 */
@Service
public class SnowflakeScoringService {

    private static final int NEUTRAL = 3;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 6;

    public AxisScore scoreValue(Map<String, Object> m, List<String> notes) {
        Double pe = num(m, "peBasicExclExtraTTM");
        Double pb = num(m, "pbAnnual");

        if (pe == null && pb == null) {
            notes.add("Value: P/E and P/B unavailable for this symbol; used neutral score.");
            return new AxisScore("Value", NEUTRAL, "No valuation ratios available from Finnhub.", true);
        }

        int score = NEUTRAL;
        StringBuilder why = new StringBuilder();

        if (pe != null) {
            // Lower P/E relative to a broad-market benchmark (~20) implies better relative value.
            if (pe > 0 && pe < 15) { score += 2; why.append("P/E ").append(round(pe)).append(" is well below the ~20 market average (+2). "); }
            else if (pe >= 15 && pe < 25) { score += 1; why.append("P/E ").append(round(pe)).append(" is near the market average (+1). "); }
            else if (pe >= 40) { score -= 2; why.append("P/E ").append(round(pe)).append(" is well above the market average (-2). "); }
            else if (pe >= 25) { score -= 1; why.append("P/E ").append(round(pe)).append(" is above the market average (-1). "); }
            else { why.append("P/E ").append(round(pe)).append(" is negative (unprofitable), no adjustment. "); }
        }

        if (pb != null) {
            if (pb > 0 && pb < 1.5) { score += 1; why.append("P/B ").append(round(pb)).append(" suggests trading near/below book value (+1). "); }
            else if (pb >= 6) { score -= 1; why.append("P/B ").append(round(pb)).append(" is high relative to book value (-1). "); }
        }

        return new AxisScore("Value", clamp(score), why.toString().trim(), false);
    }

    public AxisScore scoreFuture(Map<String, Object> m, List<String> notes) {
        Double epsGrowth = firstNonNull(num(m, "epsGrowthTTMYoy"), num(m, "epsGrowth3Y"));
        Double revGrowth = num(m, "revenueGrowth3Y");

        if (epsGrowth == null && revGrowth == null) {
            notes.add("Future: forward growth estimates unavailable; used neutral score.");
            return new AxisScore("Future", NEUTRAL, "No forward growth data available from Finnhub.", true);
        }

        int score = NEUTRAL;
        StringBuilder why = new StringBuilder();

        if (epsGrowth != null) {
            if (epsGrowth > 20) { score += 2; why.append("EPS growth ").append(round(epsGrowth)).append("% is strong (+2). "); }
            else if (epsGrowth > 5) { score += 1; why.append("EPS growth ").append(round(epsGrowth)).append("% is positive (+1). "); }
            else if (epsGrowth < -10) { score -= 2; why.append("EPS growth ").append(round(epsGrowth)).append("% is sharply negative (-2). "); }
            else if (epsGrowth < 0) { score -= 1; why.append("EPS growth ").append(round(epsGrowth)).append("% is negative (-1). "); }
        }

        if (revGrowth != null) {
            if (revGrowth > 15) { score += 1; why.append("Revenue growth ").append(round(revGrowth)).append("% is strong (+1). "); }
            else if (revGrowth < 0) { score -= 1; why.append("Revenue growth ").append(round(revGrowth)).append("% is negative (-1). "); }
        }

        return new AxisScore("Future", clamp(score), why.toString().trim(), false);
    }

    public AxisScore scorePast(Map<String, Object> m, List<String> notes) {
        Double epsGrowth5Y = num(m, "epsGrowth5Y");

        if (epsGrowth5Y == null) {
            notes.add("Past: 5-year historical earnings growth unavailable; used neutral score.");
            return new AxisScore("Past", NEUTRAL, "No historical earnings growth data available from Finnhub.", true);
        }

        int score = NEUTRAL;
        String why;
        if (epsGrowth5Y > 15) { score += 3; why = "5-year EPS growth of " + round(epsGrowth5Y) + "% is excellent (+3)."; }
        else if (epsGrowth5Y > 5) { score += 1; why = "5-year EPS growth of " + round(epsGrowth5Y) + "% is solid (+1)."; }
        else if (epsGrowth5Y < -5) { score -= 2; why = "5-year EPS growth of " + round(epsGrowth5Y) + "% shows a declining track record (-2)."; }
        else { why = "5-year EPS growth of " + round(epsGrowth5Y) + "% is roughly flat, no adjustment."; }

        return new AxisScore("Past", clamp(score), why, false);
    }

    public AxisScore scoreHealth(Map<String, Object> m, List<String> notes) {
        Double debtToEquity = num(m, "totalDebt/totalEquityAnnual");
        Double currentRatio = num(m, "currentRatioAnnual");

        if (debtToEquity == null && currentRatio == null) {
            notes.add("Health: balance sheet ratios unavailable; used neutral score.");
            return new AxisScore("Health", NEUTRAL, "No balance sheet ratios available from Finnhub.", true);
        }

        int score = NEUTRAL;
        StringBuilder why = new StringBuilder();

        if (debtToEquity != null) {
            if (debtToEquity < 0.3) { score += 2; why.append("Debt/Equity ").append(round(debtToEquity)).append(" is very low (+2). "); }
            else if (debtToEquity < 1.0) { score += 1; why.append("Debt/Equity ").append(round(debtToEquity)).append(" is manageable (+1). "); }
            else if (debtToEquity > 2.0) { score -= 2; why.append("Debt/Equity ").append(round(debtToEquity)).append(" indicates high leverage (-2). "); }
            else if (debtToEquity > 1.0) { score -= 1; why.append("Debt/Equity ").append(round(debtToEquity)).append(" is elevated (-1). "); }
        }

        if (currentRatio != null) {
            if (currentRatio > 2.0) { score += 1; why.append("Current ratio ").append(round(currentRatio)).append(" shows strong liquidity (+1). "); }
            else if (currentRatio < 1.0) { score -= 1; why.append("Current ratio ").append(round(currentRatio)).append(" is below 1, potential liquidity risk (-1). "); }
        }

        return new AxisScore("Health", clamp(score), why.toString().trim(), false);
    }

    public AxisScore scoreDividend(Map<String, Object> m, List<String> notes) {
        Double yieldPct = num(m, "dividendYieldIndicatedAnnual");
        Double payoutRatio = num(m, "payoutRatioTTM");

        if (yieldPct == null || yieldPct <= 0) {
            notes.add("Dividend: this symbol pays no dividend, or yield data is unavailable; used neutral-low score.");
            return new AxisScore("Dividend", 1, "No meaningful dividend yield reported.", yieldPct == null);
        }

        int score = NEUTRAL;
        StringBuilder why = new StringBuilder();

        if (yieldPct > 4) { score += 2; why.append("Dividend yield ").append(round(yieldPct)).append("% is high (+2). "); }
        else if (yieldPct > 1.5) { score += 1; why.append("Dividend yield ").append(round(yieldPct)).append("% is moderate (+1). "); }

        if (payoutRatio != null) {
            if (payoutRatio > 90) { score -= 2; why.append("Payout ratio ").append(round(payoutRatio)).append("% is very high, sustainability risk (-2). "); }
            else if (payoutRatio > 0 && payoutRatio < 60) { score += 1; why.append("Payout ratio ").append(round(payoutRatio)).append("% looks sustainable (+1). "); }
        } else {
            notes.add("Dividend: payout ratio unavailable; score based on yield only.");
        }

        return new AxisScore("Dividend", clamp(score), why.toString().trim(), false);
    }

    public String overallLabel(double overallScore) {
        if (overallScore >= 5.0) return "Excellent";
        if (overallScore >= 3.8) return "Good";
        if (overallScore >= 2.5) return "Average";
        if (overallScore >= 1.2) return "Weak";
        return "Poor";
    }

    // ---- helpers ----

    private int clamp(int score) {
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, score));
    }

    private Double num(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double firstNonNull(Double a, Double b) {
        return a != null ? a : b;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
