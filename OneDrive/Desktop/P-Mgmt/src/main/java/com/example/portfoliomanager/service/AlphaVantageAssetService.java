package com.example.portfoliomanager.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.example.portfoliomanager.dto.ApiDtos.AssetSearchResult;
import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.exception.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Uses Alpha Vantage's SYMBOL_SEARCH endpoint to look up tradable instruments
 * (equities, ETFs, and mutual funds) by keyword so they can be added as holdings.
 *
 * Note: Alpha Vantage's free/standard tier does not expose a dedicated
 * "mutual fund only" endpoint - SYMBOL_SEARCH returns whatever instrument type
 * it has indexed (equities, ETFs, and some mutual fund/trust tickers) under a
 * generic "type" field. We surface that type as-is so the frontend can badge
 * and filter results (e.g. ETF vs Mutual Fund vs Equity).
 */
@Service
public class AlphaVantageAssetService {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageAssetService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public AlphaVantageAssetService(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${alphavantage.api.base-url:https://www.alphavantage.co}") String baseUrl,
            @Value("${alphavantage.api.key:}") String apiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    public List<AssetSearchResult> searchSymbols(String keyword) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ExternalApiException("Alpha Vantage API key is not configured", null);
        }
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "SYMBOL_SEARCH")
                            .queryParam("keywords", keyword)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            String notice = AlphaVantageErrorUtils.extractNotice(root);
            if (notice != null) {
                throw new ExternalApiException("Alpha Vantage symbol search unavailable: " + AlphaVantageErrorUtils.summarize(notice), null);
            }

            SymbolSearchResponse response = root == null ? null : objectMapper.treeToValue(root, SymbolSearchResponse.class);

            if (response == null || response.bestMatches() == null) {
                return List.of();
            }

            return response.bestMatches().stream()
                    .map(this::toResult)
                    .toList();
        } catch (ExternalApiException exception) {
            log.warn("Alpha Vantage symbol search failed for keyword '{}': {}", keyword, exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            log.warn("Alpha Vantage symbol search failed for keyword '{}': {}", keyword, exception.getMessage());
            throw new ExternalApiException("Alpha Vantage symbol search failed: " + exception.getMessage(), exception);
        }
    }

    private AssetSearchResult toResult(SymbolMatch match) {
        Double score = null;
        try {
            if (StringUtils.hasText(match.matchScore())) {
                score = Double.valueOf(match.matchScore());
            }
        } catch (NumberFormatException ignored) {
            // leave score null if unparsable
        }
        return new AssetSearchResult(
                match.symbol(),
                match.name(),
                match.type(),
                match.region(),
                match.currency(),
                score
        );
    }

    /**
     * Fallback price lookup used when Finnhub does not support a symbol (Finnhub returns
     * 403/no data for most mutual fund tickers). Uses Alpha Vantage's GLOBAL_QUOTE function,
     * which covers equities, ETFs, and a number of mutual fund / trust tickers.
     * Returns null (rather than throwing) if no usable quote is found so callers can
     * decide how to handle missing data (e.g. require manual price entry).
     */
    public StockPriceResponse getQuote(String rawSymbol) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(rawSymbol)) {
            return null;
        }
        String symbol = rawSymbol.trim().toUpperCase();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "GLOBAL_QUOTE")
                            .queryParam("symbol", symbol)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> globalQuote = (Map<String, Object>) response.get("Global Quote");
            if (globalQuote == null || globalQuote.isEmpty()) {
                Object notice = response.containsKey("Information") ? response.get("Information")
                        : response.containsKey("Note") ? response.get("Note") : null;
                if (notice != null) {
                    log.warn("Alpha Vantage GLOBAL_QUOTE unavailable for '{}' (rate limited): {}",
                            symbol, AlphaVantageErrorUtils.summarize(notice.toString()));
                }
                return null;
            }
            Object priceObj = globalQuote.get("05. price");
            if (priceObj == null) {
                return null;
            }
            BigDecimal price = new BigDecimal(priceObj.toString());
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return new StockPriceResponse(symbol, symbol, price, "USD");
        } catch (Exception exception) {
            log.warn("Alpha Vantage GLOBAL_QUOTE fallback failed for '{}': {}", symbol, exception.getMessage());
            return null;
        }
    }

    /**
     * Historical daily close price lookup via Alpha Vantage's TIME_SERIES_DAILY function.
     * Picks the closing price of the requested date, or the nearest prior trading day if
     * the market was closed (weekend/holiday). Returns null (never throws) if no data is
     * available so callers can continue down the fallback chain.
     *
     * Note: uses outputsize=full for dates older than ~100 days since Alpha Vantage's
     * default "compact" output only covers the most recent 100 data points.
     */
    public StockPriceResponse getHistoricalQuote(String rawSymbol, java.time.LocalDate date) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(rawSymbol) || date == null) {
            return null;
        }
        String symbol = rawSymbol.trim().toUpperCase();
        boolean needsFullHistory = date.isBefore(java.time.LocalDate.now().minusDays(95));
        String outputSize = needsFullHistory ? "full" : "compact";

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "TIME_SERIES_DAILY")
                            .queryParam("symbol", symbol)
                            .queryParam("outputsize", outputSize)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> series = (Map<String, Object>) response.get("Time Series (Daily)");
            if (series == null || series.isEmpty()) {
                Object notice = response.containsKey("Information") ? response.get("Information")
                        : response.containsKey("Note") ? response.get("Note") : null;
                if (notice != null) {
                    log.warn("Alpha Vantage TIME_SERIES_DAILY unavailable for '{}' (rate limited): {}",
                            symbol, AlphaVantageErrorUtils.summarize(notice.toString()));
                }
                return null;
            }

            // Walk backwards from the requested date (up to 10 calendar days) to find the
            // nearest prior trading day with data, in case the exact date was a weekend/holiday.
            for (int i = 0; i <= 10; i++) {
                java.time.LocalDate candidate = date.minusDays(i);
                String key = candidate.toString(); // ISO yyyy-MM-dd, matches Alpha Vantage's date keys
                Object dayObj = series.get(key);
                if (dayObj instanceof Map<?, ?> dayMap) {
                    Object closeObj = dayMap.get("4. close");
                    if (closeObj != null) {
                        BigDecimal price = new BigDecimal(closeObj.toString());
                        if (price.compareTo(BigDecimal.ZERO) > 0) {
                            return new StockPriceResponse(symbol, symbol, price, "USD");
                        }
                    }
                }
            }
            return null;
        } catch (Exception exception) {
            log.warn("Alpha Vantage TIME_SERIES_DAILY lookup failed for '{}' on {}: {}", symbol, date, exception.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SymbolSearchResponse(
            @JsonProperty("bestMatches") List<SymbolMatch> bestMatches
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SymbolMatch(
            @JsonProperty("1. symbol") String symbol,
            @JsonProperty("2. name") String name,
            @JsonProperty("3. type") String type,
            @JsonProperty("4. region") String region,
            @JsonProperty("8. currency") String currency,
            @JsonProperty("9. matchScore") String matchScore
    ) {
    }
}
