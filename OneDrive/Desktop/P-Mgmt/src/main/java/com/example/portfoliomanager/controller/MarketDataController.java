package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.NewsArticle;
import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.exception.ApiError;
import com.example.portfoliomanager.service.FinnhubStockService;
import com.example.portfoliomanager.service.NewsService;
import com.example.portfoliomanager.service.YahooFinanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Market Data", description = "Endpoints for stock quotes and financial news")
public class MarketDataController {

    private static final Logger log = LoggerFactory.getLogger(MarketDataController.class);

    private final YahooFinanceService yahooFinanceService;
    private final FinnhubStockService finnhubStockService;
    private final NewsService newsService;

    public MarketDataController(YahooFinanceService yahooFinanceService, FinnhubStockService finnhubStockService, NewsService newsService) {
        this.yahooFinanceService = yahooFinanceService;
        this.finnhubStockService = finnhubStockService;
        this.newsService = newsService;
    }

    @GetMapping("/stocks/{symbol}")
        @Operation(
            summary = "Get stock quote",
            description = "Fetches latest market quote information for a stock symbol.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quote fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid symbol",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "External market API error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public StockPriceResponse getStockPrice(
            @Parameter(description = "Stock ticker symbol", example = "AAPL", required = true)
            @PathVariable String symbol) {
        return yahooFinanceService.getQuote(symbol);
    }

    @GetMapping("/market/indices")
    @Operation(
        summary = "Get market ticker tape data",
        description = "Fetches real-time quotes for major US indices and stocks via Finnhub.")
    public List<StockPriceResponse> getMarketIndices() {
        // Simple mapping: Finnhub symbol -> display name
        // SPY = S&P 500 ETF, DIA = Dow Jones ETF, QQQ = NASDAQ ETF
        var tickers = new java.util.LinkedHashMap<String, String>();
        tickers.put("SPY",  "S&P 500");
        tickers.put("DIA",  "Dow Jones");
        tickers.put("QQQ",  "NASDAQ");
        tickers.put("AAPL", "Apple");
        tickers.put("TSLA", "Tesla");
        tickers.put("MSFT", "Microsoft");

        List<StockPriceResponse> results = new java.util.ArrayList<>();
        for (var entry : tickers.entrySet()) {
            try {
                StockPriceResponse quote = finnhubStockService.getQuote(entry.getKey());
                results.add(new StockPriceResponse(
                    entry.getKey(),
                    entry.getValue(),
                    quote.price(),
                    "USD"
                ));
            } catch (Exception e) {
                log.warn("Finnhub quote failed for {}: {}", entry.getKey(), e.getMessage());
                results.add(new StockPriceResponse(
                    entry.getKey(),
                    entry.getValue(),
                    java.math.BigDecimal.ZERO,
                    "USD"
                ));
            }
        }
        return results;
    }


    @GetMapping("/news")
        @Operation(
            summary = "Get cached financial news",
            description = "Returns currently cached financial news articles.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "News fetched successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "External news API error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
    public List<NewsArticle> getNews() {
        return newsService.getCachedArticles();
    }

    @PostMapping("/news/refresh")
        @Operation(
            summary = "Refresh financial news",
            description = "Forces a refresh from the upstream news provider and returns updated cache.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "News refreshed successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "External news API error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
    public List<NewsArticle> refreshNews() {
        return newsService.refresh();
    }
}
