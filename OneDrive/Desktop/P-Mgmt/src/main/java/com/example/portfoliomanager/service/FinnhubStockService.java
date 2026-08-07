package com.example.portfoliomanager.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.exception.ExternalApiException;

@Service
public class FinnhubStockService {

    private static final Logger log = LoggerFactory.getLogger(FinnhubStockService.class);

    private final RestClient restClient;
    private final String apiKey;

    public FinnhubStockService(
            RestClient.Builder restClientBuilder,
            @Value("${finnhub.api.base-url:https://finnhub.io/api/v1}") String baseUrl,
            @Value("${finnhub.api.key:}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public StockPriceResponse getQuote(String rawSymbol) {
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        if (!isConfigured()) {
            throw new ExternalApiException("Finnhub API key is not configured. Please set FINNHUB_API_KEY in your .env file.", null);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> quoteResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", symbol)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (quoteResponse == null || !quoteResponse.containsKey("c")) {
                throw new ExternalApiException("No quote data found for symbol: " + symbol, null);
            }

            Object currentPriceObj = quoteResponse.get("c");
            if (currentPriceObj == null) {
                throw new ExternalApiException("Price not available for symbol: " + symbol, null);
            }

            BigDecimal price = new BigDecimal(currentPriceObj.toString());
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ExternalApiException("Invalid or zero price returned for symbol: " + symbol, null);
            }

            String companyName = symbol;
            String currency = "USD";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> profileResponse = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/stock/profile2")
                                .queryParam("symbol", symbol)
                                .queryParam("token", apiKey)
                                .build())
                        .retrieve()
                        .body(Map.class);

                if (profileResponse != null) {
                    if (profileResponse.get("name") != null && !profileResponse.get("name").toString().isBlank()) {
                        companyName = profileResponse.get("name").toString();
                    }
                    if (profileResponse.get("currency") != null && !profileResponse.get("currency").toString().isBlank()) {
                        currency = profileResponse.get("currency").toString();
                    }
                }
            } catch (Exception e) {
                log.debug("Could not fetch company profile for {}: {}", symbol, e.getMessage());
            }

            return new StockPriceResponse(symbol, companyName, price, currency);

        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalApiException("Finnhub API request failed for " + symbol + ": " + e.getMessage(), e);
        }
    }

    public StockPriceResponse getHistoricalQuote(String rawSymbol, java.time.LocalDate purchaseDate) {
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        if (purchaseDate == null || purchaseDate.isEqual(java.time.LocalDate.now()) || purchaseDate.isAfter(java.time.LocalDate.now())) {
            return getQuote(symbol);
        }

        if (!isConfigured()) {
            return null;
        }

        try {
            java.time.ZoneId zone = java.time.ZoneId.of("UTC");
            long toEpoch = purchaseDate.atTime(23, 59, 59).atZone(zone).toEpochSecond();
            long fromEpoch = purchaseDate.minusDays(7).atStartOfDay(zone).toEpochSecond();

            @SuppressWarnings("unchecked")
            Map<String, Object> candleResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stock/candle")
                            .queryParam("symbol", symbol)
                            .queryParam("resolution", "D")
                            .queryParam("from", fromEpoch)
                            .queryParam("to", toEpoch)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            if (candleResponse != null && "ok".equals(candleResponse.get("s"))) {
                Object closePricesObj = candleResponse.get("c");
                if (closePricesObj instanceof java.util.List<?> prices && !prices.isEmpty()) {
                    Object lastPriceObj = prices.get(prices.size() - 1);
                    if (lastPriceObj != null) {
                        BigDecimal histPrice = new BigDecimal(lastPriceObj.toString());
                        if (histPrice.compareTo(BigDecimal.ZERO) > 0) {
                            String companyName = symbol;
                            try {
                                StockPriceResponse live = getQuote(symbol);
                                companyName = live.companyName();
                            } catch (Exception ignored) {}
                            log.info("Finnhub historical quote for {} on {}: {}", symbol, purchaseDate, histPrice);
                            return new StockPriceResponse(symbol, companyName, histPrice, "USD");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Finnhub historical candle fetch failed for {} on {}: {}", symbol, purchaseDate, e.getMessage());
        }

        return null;
    }

    /**
     * Fetches OHLCV candlestick history for a symbol, for use in TradingView-style charts.
     *
     * @param rawSymbol  ticker symbol
     * @param resolution Finnhub resolution: 1, 5, 15, 30, 60, D, W, M
     * @param days       how many days back from "now" to fetch
     */
    public com.example.portfoliomanager.dto.ApiDtos.CandleResponse getCandles(String rawSymbol, String resolution, int days) {
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        if (!isConfigured()) {
            throw new ExternalApiException("Finnhub API key is not configured. Please set FINNHUB_API_KEY in your .env file.", null);
        }

        String res = (resolution == null || resolution.isBlank()) ? "D" : resolution;
        long toEpoch = java.time.Instant.now().getEpochSecond();
        long fromEpoch = java.time.Instant.now().minus(java.time.Duration.ofDays(Math.max(days, 1))).getEpochSecond();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> candleResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stock/candle")
                            .queryParam("symbol", symbol)
                            .queryParam("resolution", res)
                            .queryParam("from", fromEpoch)
                            .queryParam("to", toEpoch)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(Map.class);

            java.util.List<com.example.portfoliomanager.dto.ApiDtos.CandlePoint> points = new java.util.ArrayList<>();
            if (candleResponse != null && "ok".equals(candleResponse.get("s"))) {
                java.util.List<?> opens = (java.util.List<?>) candleResponse.get("o");
                java.util.List<?> highs = (java.util.List<?>) candleResponse.get("h");
                java.util.List<?> lows = (java.util.List<?>) candleResponse.get("l");
                java.util.List<?> closes = (java.util.List<?>) candleResponse.get("c");
                java.util.List<?> volumes = (java.util.List<?>) candleResponse.get("v");
                java.util.List<?> times = (java.util.List<?>) candleResponse.get("t");

                if (times != null) {
                    for (int i = 0; i < times.size(); i++) {
                        long t = Long.parseLong(times.get(i).toString());
                        BigDecimal o = new BigDecimal(opens.get(i).toString());
                        BigDecimal h = new BigDecimal(highs.get(i).toString());
                        BigDecimal l = new BigDecimal(lows.get(i).toString());
                        BigDecimal c = new BigDecimal(closes.get(i).toString());
                        long v = Long.parseLong(volumes.get(i).toString());
                        points.add(new com.example.portfoliomanager.dto.ApiDtos.CandlePoint(t, o, h, l, c, v));
                    }
                }
            } else {
                log.warn("Finnhub candle response for {} was not ok: {}", symbol, candleResponse);
            }

            return new com.example.portfoliomanager.dto.ApiDtos.CandleResponse(symbol, res, points);
        } catch (Exception e) {
            throw new ExternalApiException("Failed to fetch candle data for " + symbol + ": " + e.getMessage(), e);
        }
    }
}
