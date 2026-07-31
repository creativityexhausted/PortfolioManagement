package com.example.portfoliomanager.service;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FinnhubQuoteResponse(Double c) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FinnhubProfileResponse(String name, String currency) {
    }
}
