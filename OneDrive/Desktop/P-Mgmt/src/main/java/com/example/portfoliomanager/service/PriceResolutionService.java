package com.example.portfoliomanager.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;

/**
 * Central price-lookup coordinator for the Holdings feature. Enforces a strict,
 * fail-fast provider order — Finnhub, then Alpha Vantage, then Yahoo Finance —
 * for both historical (purchase-date) and live/current price lookups, and
 * records exactly which provider (if any) actually supplied the data so the
 * outcome can be surfaced to the user via notifications.
 *
 * Each provider attempt is wrapped individually so that one provider's failure
 * (exception, empty response, zero/negative price) never blocks the next
 * provider from being tried, and never silently corrupts the result (e.g. a
 * "live price" fallback is never used to satisfy a "historical price" request).
 */
@Service
public class PriceResolutionService {

    private static final Logger log = LoggerFactory.getLogger(PriceResolutionService.class);

    public static final String SOURCE_FINNHUB = "FINNHUB";
    public static final String SOURCE_ALPHA_VANTAGE = "ALPHA_VANTAGE";
    public static final String SOURCE_YAHOO = "YAHOO";
    public static final String SOURCE_NONE = "NONE";

    private final FinnhubStockService finnhubStockService;
    private final AlphaVantageAssetService alphaVantageAssetService;
    private final YahooFinanceService yahooFinanceService;

    public PriceResolutionService(
            FinnhubStockService finnhubStockService,
            AlphaVantageAssetService alphaVantageAssetService,
            YahooFinanceService yahooFinanceService) {
        this.finnhubStockService = finnhubStockService;
        this.alphaVantageAssetService = alphaVantageAssetService;
        this.yahooFinanceService = yahooFinanceService;
    }

    /** Result of a price lookup attempt across all providers. */
    public record PriceLookupResult(
            StockPriceResponse quote,
            String source,
            List<String> notes
    ) {
        public boolean found() {
            return quote != null && quote.price() != null && quote.price().compareTo(BigDecimal.ZERO) > 0;
        }
    }

    /**
     * Resolves the price a stock traded at on (or nearest before) the given date.
     * Order: Finnhub candle data -> Alpha Vantage daily time series -> Yahoo Finance chart history.
     * If {@code date} is null/today/future, this degrades to a live-price lookup automatically
     * (each provider's own historical method already handles that internally).
     */
    public PriceLookupResult resolveHistoricalPrice(String symbol, LocalDate date) {
        List<String> notes = new ArrayList<>();

        StockPriceResponse finnhubResult = attempt(SOURCE_FINNHUB, notes,
                () -> finnhubStockService.getHistoricalQuote(symbol, date));
        if (isUsable(finnhubResult)) {
            notes.add("Finnhub provided the historical price for " + symbol + " on " + date + ".");
            return new PriceLookupResult(finnhubResult, SOURCE_FINNHUB, notes);
        }

        StockPriceResponse alphaResult = attempt(SOURCE_ALPHA_VANTAGE, notes,
                () -> alphaVantageAssetService.getHistoricalQuote(symbol, date));
        if (isUsable(alphaResult)) {
            notes.add("Finnhub unavailable — Alpha Vantage provided the historical price for " + symbol + " on " + date + ".");
            return new PriceLookupResult(alphaResult, SOURCE_ALPHA_VANTAGE, notes);
        }

        StockPriceResponse yahooResult = attempt(SOURCE_YAHOO, notes,
                () -> yahooFinanceService.getHistoricalQuote(symbol, date));
        if (isUsable(yahooResult)) {
            notes.add("Finnhub and Alpha Vantage unavailable — Yahoo Finance provided the historical price for " + symbol + " on " + date + ".");
            return new PriceLookupResult(yahooResult, SOURCE_YAHOO, notes);
        }

        notes.add("No provider (Finnhub, Alpha Vantage, Yahoo) could supply a historical price for " + symbol + " on " + date + ".");
        return new PriceLookupResult(null, SOURCE_NONE, notes);
    }

    /**
     * Resolves the current/live market price for a symbol.
     * Order: Finnhub -> Alpha Vantage (GLOBAL_QUOTE) -> Yahoo Finance (chart API, Finnhub-skip variant).
     */
    public PriceLookupResult resolveLivePrice(String symbol) {
        List<String> notes = new ArrayList<>();

        StockPriceResponse finnhubResult = attempt(SOURCE_FINNHUB, notes,
                () -> finnhubStockService.getQuote(symbol));
        if (isUsable(finnhubResult)) {
            notes.add("Finnhub provided the live price for " + symbol + ".");
            return new PriceLookupResult(finnhubResult, SOURCE_FINNHUB, notes);
        }

        StockPriceResponse alphaResult = attempt(SOURCE_ALPHA_VANTAGE, notes,
                () -> alphaVantageAssetService.getQuote(symbol));
        if (isUsable(alphaResult)) {
            notes.add("Finnhub unavailable — Alpha Vantage provided the live price for " + symbol + ".");
            return new PriceLookupResult(alphaResult, SOURCE_ALPHA_VANTAGE, notes);
        }

        // Use the Yahoo-only path here (skips its internal Finnhub pre-check, which we already
        // attempted above) to avoid a redundant/wasted duplicate Finnhub call.
        StockPriceResponse yahooResult = attempt(SOURCE_YAHOO, notes,
                () -> yahooFinanceService.getQuoteYahooOnly(symbol));
        if (isUsable(yahooResult)) {
            notes.add("Finnhub and Alpha Vantage unavailable — Yahoo Finance provided the live price for " + symbol + ".");
            return new PriceLookupResult(yahooResult, SOURCE_YAHOO, notes);
        }

        notes.add("No provider (Finnhub, Alpha Vantage, Yahoo) could supply a live price for " + symbol + ".");
        return new PriceLookupResult(null, SOURCE_NONE, notes);
    }

    private boolean isUsable(StockPriceResponse quote) {
        return quote != null && quote.price() != null && quote.price().compareTo(BigDecimal.ZERO) > 0;
    }

    private StockPriceResponse attempt(String providerName, List<String> notes, java.util.function.Supplier<StockPriceResponse> call) {
        try {
            StockPriceResponse result = call.get();
            if (!isUsable(result)) {
                notes.add(providerName + " returned no usable price.");
                log.debug("{} returned no usable price.", providerName);
            }
            return result;
        } catch (Exception exception) {
            notes.add(providerName + " lookup failed: " + exception.getMessage());
            log.warn("{} lookup failed: {}", providerName, exception.getMessage());
            return null;
        }
    }
}
