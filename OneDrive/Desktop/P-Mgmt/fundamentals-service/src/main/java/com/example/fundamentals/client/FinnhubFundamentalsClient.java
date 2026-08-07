package com.example.fundamentals.client;

import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.example.fundamentals.exception.ExternalApiException;

/**
 * Thin client around Finnhub's `/stock/metric?metric=all` endpoint, which returns a large
 * bag of fundamental ratios (P/E, P/B, debt/equity, margins, growth, dividend yield, etc.)
 * for a symbol in a single call. This is the only external API this microservice depends on.
 */
@Component
public class FinnhubFundamentalsClient {

    private static final Logger log = LoggerFactory.getLogger(FinnhubFundamentalsClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public FinnhubFundamentalsClient(
            RestClient.Builder restClientBuilder,
            @Value("${finnhub.api.base-url}") String baseUrl,
            @Value("${finnhub.api.key:}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    /**
     * Fetches the raw "metric" map from Finnhub's basic financials endpoint.
     * Keys of interest (not guaranteed present for every symbol):
     *   peBasicExclExtraTTM, pbAnnual, roeTTM, roiAnnual,
     *   totalDebt/totalEquityAnnual, currentRatioAnnual,
     *   epsGrowth3Y, epsGrowth5Y, epsGrowthTTMYoy, revenueGrowth3Y, revenueGrowth5Y,
     *   dividendYieldIndicatedAnnual, payoutRatioTTM, beta
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetrics(String rawSymbol) {
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        if (!isConfigured()) {
            throw new ExternalApiException(
                    "Finnhub API key is not configured. Please set FINNHUB_API_KEY in fundamentals-service/.env.", null);
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stock/metric")
                            .queryParam("symbol", symbol)
                            .queryParam("metric", "all")
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("metric")) {
                throw new ExternalApiException("Finnhub returned no fundamentals data for symbol: " + symbol, null);
            }

            Object metricObj = response.get("metric");
            if (!(metricObj instanceof Map)) {
                throw new ExternalApiException("Finnhub fundamentals response was malformed for symbol: " + symbol, null);
            }

            return (Map<String, Object>) metricObj;
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Finnhub fundamentals lookup failed for {}: {}", symbol, e.getMessage());
            throw new ExternalApiException("Finnhub fundamentals lookup failed for symbol: " + symbol, e);
        }
    }
}
