package com.example.portfoliomanager.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.portfoliomanager.dto.ApiDtos.StockHistoryPoint;
import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.exception.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Service
public class YahooFinanceService {

    private final String apiKey;
    private final RestClient restClient;

    public YahooFinanceService(
            @Value("${finnhub.api.base-url:https://finnhub.io/api/v1}") String baseUrl,
            @Value("${finnhub.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public StockPriceResponse getQuote(String rawSymbol) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ExternalApiException("FINNHUB_API_KEY is not configured", null);
        }

        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        try {
            FinnhubQuoteResponse quote = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/quote")
                            .queryParam("symbol", symbol)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubQuoteResponse.class);

            if (quote == null || quote.c() == null || quote.c() <= 0) {
                throw new ExternalApiException("No Finnhub quote found for " + symbol, null);
            }

            FinnhubProfileResponse profile = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/stock/profile2")
                            .queryParam("symbol", symbol)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubProfileResponse.class);

            String companyName = profile == null || profile.name() == null || profile.name().isBlank()
                    ? symbol
                    : profile.name();
            String currency = profile == null || profile.currency() == null || profile.currency().isBlank()
                    ? "USD"
                    : profile.currency();

            return new StockPriceResponse(
                    symbol,
                    companyName,
                    BigDecimal.valueOf(quote.c()),
                    currency);
        } catch (RuntimeException exception) {
            throw new ExternalApiException("Finnhub request failed for " + symbol, exception);
        }
    }

        public List<StockHistoryPoint> getHistory(String rawSymbol, int days) {
                if (apiKey == null || apiKey.isBlank()) {
                        throw new ExternalApiException("FINNHUB_API_KEY is not configured", null);
                }

                String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
                int lookbackDays = Math.max(1, Math.min(days, 90));
                long to = Instant.now().getEpochSecond();
                long from = Instant.now().minus(lookbackDays, ChronoUnit.DAYS).getEpochSecond();

                try {
                        FinnhubCandleResponse candles = restClient.get()
                                        .uri(uriBuilder -> uriBuilder.path("/stock/candle")
                                                        .queryParam("symbol", symbol)
                                                        .queryParam("resolution", "D")
                                                        .queryParam("from", from)
                                                        .queryParam("to", to)
                                                        .queryParam("token", apiKey)
                                                        .build())
                                        .retrieve()
                                        .body(FinnhubCandleResponse.class);

                        if (candles == null || candles.t() == null || candles.c() == null || candles.t().isEmpty() || candles.c().isEmpty()) {
                                return fallbackHistoryFromQuote(symbol, from, to);
                        }

                        int size = Math.min(candles.t().size(), candles.c().size());
                        List<StockHistoryPoint> points = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                                Long timestamp = candles.t().get(i);
                                Double close = candles.c().get(i);
                                if (timestamp == null || close == null || close <= 0) {
                                        continue;
                                }
                                points.add(new StockHistoryPoint(timestamp, BigDecimal.valueOf(close)));
                        }
                        if (points.isEmpty()) {
                                return fallbackHistoryFromQuote(symbol, from, to);
                        }
                        return points;
                } catch (RuntimeException exception) {
                        throw new ExternalApiException("Finnhub history request failed for " + symbol, exception);
                }
        }

        private List<StockHistoryPoint> fallbackHistoryFromQuote(String symbol, long from, long to) {
                FinnhubQuoteResponse quote = restClient.get()
                                .uri(uriBuilder -> uriBuilder.path("/quote")
                                                .queryParam("symbol", symbol)
                                                .queryParam("token", apiKey)
                                                .build())
                                .retrieve()
                                .body(FinnhubQuoteResponse.class);

                if (quote == null || quote.c() == null || quote.pc() == null || quote.c() <= 0 || quote.pc() <= 0) {
                        return List.of();
                }

                long start = Math.max(0L, from);
                long end = Math.max(start + 1, to);
                return List.of(
                                new StockHistoryPoint(start, BigDecimal.valueOf(quote.pc())),
                                new StockHistoryPoint(end, BigDecimal.valueOf(quote.c())));
        }

    @JsonIgnoreProperties(ignoreUnknown = true)
        private record FinnhubQuoteResponse(Double c, Double pc) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FinnhubProfileResponse(String name, String currency) {
    }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record FinnhubCandleResponse(String s, List<Double> c, List<Long> t) {
        }
}
