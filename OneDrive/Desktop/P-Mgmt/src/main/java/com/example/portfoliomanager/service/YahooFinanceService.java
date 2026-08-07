package com.example.portfoliomanager.service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.exception.ExternalApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class YahooFinanceService {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceService.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FinnhubStockService finnhubStockService;

    public YahooFinanceService(FinnhubStockService finnhubStockService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.finnhubStockService = finnhubStockService;
    }

    public StockPriceResponse getQuote(String rawSymbol) {
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);

        // Prioritize Finnhub if configured
        if (finnhubStockService.isConfigured()) {
            try {
                StockPriceResponse quote = finnhubStockService.getQuote(symbol);
                log.info("Finnhub live quote for {}: {} {}", symbol, quote.price(), quote.currency());
                return quote;
            } catch (Exception e) {
                log.warn("Finnhub lookup failed for {}, falling back to Yahoo Finance: {}", symbol, e.getMessage());
            }
        }

        return getQuoteYahooOnly(symbol);
    }

    /**
     * Fetches the live quote directly from Yahoo Finance's public chart API, skipping the
     * Finnhub pre-check. Used by {@link PriceResolutionService} which already attempted
     * Finnhub as its own explicit step, to avoid a duplicate/wasted Finnhub call.
     */
    public StockPriceResponse getQuoteYahooOnly(String rawSymbol) {
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        try {
            // Use URI template variable so special chars like ^ (%5E) are properly percent-encoded
            URI uri = UriComponentsBuilder
                    .fromHttpUrl("https://query1.finance.yahoo.com/v8/finance/chart/{symbol}")
                    .queryParam("interval", "1d")
                    .queryParam("range", "1d")
                    .buildAndExpand(symbol)
                    .toUri();

            log.info("Yahoo Finance request URI: {}", uri);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode resultArr = root.path("chart").path("result");
                if (resultArr.isArray() && !resultArr.isEmpty()) {
                    JsonNode metaNode = resultArr.get(0).path("meta");
                    if (!metaNode.isMissingNode()) {
                        double priceVal = metaNode.path("regularMarketPrice").asDouble(0.0);
                        if (priceVal == 0.0) {
                            priceVal = metaNode.path("chartPreviousClose").asDouble(0.0);
                        }
                        String currency = metaNode.path("currency").asText("USD");
                        String name = metaNode.path("shortName").asText(metaNode.path("longName").asText(symbol));

                        if (priceVal > 0) {
                            BigDecimal price = BigDecimal.valueOf(priceVal);
                            log.info("Yahoo quote for {}: {} {}", symbol, price, currency);
                            return new StockPriceResponse(symbol, name, price, currency);
                        } else {
                            log.warn("Yahoo returned price=0 for {}. Meta: currency={}, name={}", symbol, currency, name);
                        }
                    } else {
                        log.warn("Yahoo chart result has no 'meta' node for {}", symbol);
                    }
                } else {
                    // Check for error in response
                    JsonNode errorNode = root.path("chart").path("error");
                    if (!errorNode.isMissingNode()) {
                        log.warn("Yahoo chart API error for {}: {}", symbol, errorNode);
                    } else {
                        log.warn("Yahoo chart returned empty result array for {}", symbol);
                    }
                }
            }
        } catch (Exception exception) {
            log.warn("Yahoo REST chart lookup failed for {}: {}", symbol, exception.getMessage());
        }

        throw new ExternalApiException("Could not fetch market quote for ticker symbol: " + symbol, null);
    }

    public StockPriceResponse getHistoricalQuote(String rawSymbol, java.time.LocalDate purchaseDate) {
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        if (purchaseDate == null || purchaseDate.isEqual(java.time.LocalDate.now()) || purchaseDate.isAfter(java.time.LocalDate.now())) {
            return getQuote(symbol);
        }

        // Query Yahoo Finance chart API for historical candles around purchaseDate
        try {
            java.time.ZoneId zone = java.time.ZoneId.of("UTC");
            long period1 = purchaseDate.minusDays(5).atStartOfDay(zone).toEpochSecond();
            long period2 = purchaseDate.plusDays(3).atTime(23, 59, 59).atZone(zone).toEpochSecond();

            String[] hosts = {"query2.finance.yahoo.com", "query1.finance.yahoo.com"};
            for (String host : hosts) {
                try {
                    URI uri = UriComponentsBuilder
                            .fromHttpUrl("https://" + host + "/v8/finance/chart/{symbol}")
                            .queryParam("period1", period1)
                            .queryParam("period2", period2)
                            .queryParam("interval", "1d")
                            .buildAndExpand(symbol)
                            .toUri();

                    log.info("Yahoo Finance historical URI for {} on {} via {}: {}", symbol, purchaseDate, host, uri);

                    HttpHeaders headers = new HttpHeaders();
                    // Generate a slightly randomized user agent to help avoid 429s
                    String[] userAgents = {
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:123.0) Gecko/20100101 Firefox/123.0",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Safari/605.1.15"
                    };
                    String randomAgent = userAgents[new java.util.Random().nextInt(userAgents.length)];
                    headers.set("User-Agent", randomAgent);
                    headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
                    headers.set("Accept-Language", "en-US,en;q=0.5");
                    headers.set("Connection", "keep-alive");
                    headers.set("Upgrade-Insecure-Requests", "1");
                    headers.set("Sec-Fetch-Dest", "document");
                    headers.set("Sec-Fetch-Mode", "navigate");
                    headers.set("Sec-Fetch-Site", "none");
                    headers.set("Sec-Fetch-User", "?1");
                    
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        JsonNode root = objectMapper.readTree(response.getBody());
                        JsonNode resultArr = root.path("chart").path("result");
                        if (resultArr.isArray() && !resultArr.isEmpty()) {
                            JsonNode resultObj = resultArr.get(0);
                            JsonNode metaNode = resultObj.path("meta");
                            String companyName = metaNode.path("shortName").asText(metaNode.path("longName").asText(symbol));
                            String currency = metaNode.path("currency").asText("USD");

                            JsonNode quoteObj = resultObj.path("indicators").path("quote");
                            if (quoteObj.isArray() && !quoteObj.isEmpty()) {
                                JsonNode closeArr = quoteObj.get(0).path("close");
                                if (closeArr.isArray() && !closeArr.isEmpty()) {
                                    for (int i = closeArr.size() - 1; i >= 0; i--) {
                                        JsonNode valNode = closeArr.get(i);
                                        if (valNode.isNumber() && !valNode.isNull()) {
                                            double histVal = valNode.asDouble();
                                            if (histVal > 0) {
                                                BigDecimal price = BigDecimal.valueOf(histVal).setScale(4, java.math.RoundingMode.HALF_UP);
                                                log.info("Yahoo historical price for {} on {}: {} {}", symbol, purchaseDate, price, currency);
                                                return new StockPriceResponse(symbol, companyName, price, currency);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Yahoo historical lookup via {} failed for {} on {}: {}", host, symbol, purchaseDate, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to build Yahoo historical query for {} on {}: {}", symbol, purchaseDate, e.getMessage());
        }

        // Return null if historical quote cannot be fetched instead of falling back to live quote
        return null;
    }

    /**
     * Fetches OHLCV candlestick history for a symbol from Yahoo Finance's public chart API
     * (no API key required). Used as the primary/fallback data source for TradingView-style charts
     * since Finnhub's free tier restricts the /stock/candle endpoint.
     *
     * @param rawSymbol ticker symbol
     * @param range     Yahoo range string, e.g. "1d", "5d", "1mo", "6mo", "1y"
     * @param interval  Yahoo interval string, e.g. "5m", "30m", "1d"
     */
    public com.example.portfoliomanager.dto.ApiDtos.CandleResponse getCandles(String rawSymbol, String range, String interval) {
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        String[] hosts = {"query1.finance.yahoo.com", "query2.finance.yahoo.com"};
        Exception lastError = null;

        for (String host : hosts) {
            try {
                URI uri = UriComponentsBuilder
                        .fromHttpUrl("https://" + host + "/v8/finance/chart/{symbol}")
                        .queryParam("range", range)
                        .queryParam("interval", interval)
                        .buildAndExpand(symbol)
                        .toUri();

                String[] userAgents = {
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:123.0) Gecko/20100101 Firefox/123.0",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Safari/605.1.15"
                };
                String randomAgent = userAgents[new java.util.Random().nextInt(userAgents.length)];

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", randomAgent);
                headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
                headers.set("Accept-Language", "en-US,en;q=0.5");
                headers.set("Connection", "keep-alive");
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode resultArr = root.path("chart").path("result");
                    if (resultArr.isArray() && !resultArr.isEmpty()) {
                        JsonNode resultObj = resultArr.get(0);
                        JsonNode timestamps = resultObj.path("timestamp");
                        JsonNode quoteObj = resultObj.path("indicators").path("quote");

                        java.util.List<com.example.portfoliomanager.dto.ApiDtos.CandlePoint> points = new java.util.ArrayList<>();
                        if (timestamps.isArray() && quoteObj.isArray() && !quoteObj.isEmpty()) {
                            JsonNode quote0 = quoteObj.get(0);
                            JsonNode opens = quote0.path("open");
                            JsonNode highs = quote0.path("high");
                            JsonNode lows = quote0.path("low");
                            JsonNode closes = quote0.path("close");
                            JsonNode volumes = quote0.path("volume");

                            for (int i = 0; i < timestamps.size(); i++) {
                                JsonNode closeNode = closes.path(i);
                                if (closeNode.isNull() || !closeNode.isNumber()) {
                                    continue;
                                }
                                long t = timestamps.get(i).asLong();
                                BigDecimal o = toDecimal(opens.path(i), closeNode);
                                BigDecimal h = toDecimal(highs.path(i), closeNode);
                                BigDecimal l = toDecimal(lows.path(i), closeNode);
                                BigDecimal c = BigDecimal.valueOf(closeNode.asDouble());
                                long v = volumes.path(i).isNumber() ? volumes.path(i).asLong() : 0L;
                                points.add(new com.example.portfoliomanager.dto.ApiDtos.CandlePoint(t, o, h, l, c, v));
                            }
                        }
                        return new com.example.portfoliomanager.dto.ApiDtos.CandleResponse(symbol, interval, points);
                    }
                }
            } catch (Exception e) {
                lastError = e;
                log.warn("Yahoo candle fetch via {} failed for {}: {}", host, symbol, e.getMessage());
            }
        }

        throw new ExternalApiException("Could not fetch candle data for ticker symbol: " + symbol, lastError);
    }

    private static BigDecimal toDecimal(JsonNode node, JsonNode fallback) {
        JsonNode use = (node != null && node.isNumber()) ? node : fallback;
        return BigDecimal.valueOf(use.asDouble());
    }
}
