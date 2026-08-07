package com.example.fundamentals.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.fundamentals.client.FinnhubFundamentalsClient;
import com.example.fundamentals.dto.SnowflakeDtos.AxisScore;
import com.example.fundamentals.dto.SnowflakeDtos.SnowflakeScore;

/**
 * Orchestrates fetching Finnhub fundamentals, running them through the scoring rubric, and
 * caching the result per-symbol. Fundamentals change slowly (quarterly earnings releases),
 * so a long TTL (default 24h) is used to conserve Finnhub's free-tier quota, in contrast to
 * the main app's 5-minute live-price refresh cycle.
 */
@Service
public class SnowflakeService {

    private record CacheEntry(SnowflakeScore score, long fetchedAtMs) {
    }

    private final FinnhubFundamentalsClient client;
    private final SnowflakeScoringService scoringService;
    private final long ttlMs;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public SnowflakeService(
            FinnhubFundamentalsClient client,
            SnowflakeScoringService scoringService,
            @Value("${snowflake.cache.ttl-ms:86400000}") long ttlMs) {
        this.client = client;
        this.scoringService = scoringService;
        this.ttlMs = ttlMs;
    }

    public SnowflakeScore getScore(String rawSymbol) {
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);

        CacheEntry cached = cache.get(symbol);
        if (cached != null && (System.currentTimeMillis() - cached.fetchedAtMs()) < ttlMs) {
            return cached.score();
        }

        SnowflakeScore fresh = compute(symbol);
        cache.put(symbol, new CacheEntry(fresh, System.currentTimeMillis()));
        return fresh;
    }

    private SnowflakeScore compute(String symbol) {
        List<String> notes = new ArrayList<>();
        Map<String, Object> metrics = client.getMetrics(symbol);

        AxisScore value = scoringService.scoreValue(metrics, notes);
        AxisScore future = scoringService.scoreFuture(metrics, notes);
        AxisScore past = scoringService.scorePast(metrics, notes);
        AxisScore health = scoringService.scoreHealth(metrics, notes);
        AxisScore dividend = scoringService.scoreDividend(metrics, notes);

        double overall = (value.score() + future.score() + past.score() + health.score() + dividend.score()) / 5.0;
        String label = scoringService.overallLabel(overall);

        return new SnowflakeScore(
                symbol,
                symbol, // company name enrichment left to the frontend/main app which already has it
                value, future, past, health, dividend,
                Math.round(overall * 100.0) / 100.0,
                label,
                metrics,
                notes);
    }
}
