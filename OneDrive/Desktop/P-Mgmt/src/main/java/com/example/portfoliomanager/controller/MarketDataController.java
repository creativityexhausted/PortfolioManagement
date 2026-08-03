package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.chatbot.NewsAiEnrichmentService;
import com.example.portfoliomanager.chatbot.PortfolioAssistantContextService;
import com.example.portfoliomanager.dto.ApiDtos.AssetSearchResult;
import com.example.portfoliomanager.dto.ApiDtos.CandleResponse;
import com.example.portfoliomanager.dto.ApiDtos.NewsArticle;
import com.example.portfoliomanager.dto.ApiDtos.NewsPortfolioBrief;
import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.exception.ApiError;
import com.example.portfoliomanager.service.AlphaVantageAssetService;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    private final NewsAiEnrichmentService newsAiEnrichmentService;
    private final PortfolioAssistantContextService portfolioAssistantContextService;
    private final AlphaVantageAssetService alphaVantageAssetService;

    public MarketDataController(
            YahooFinanceService yahooFinanceService,
            FinnhubStockService finnhubStockService,
            NewsService newsService,
            NewsAiEnrichmentService newsAiEnrichmentService,
            PortfolioAssistantContextService portfolioAssistantContextService,
            AlphaVantageAssetService alphaVantageAssetService) {
        this.yahooFinanceService = yahooFinanceService;
        this.finnhubStockService = finnhubStockService;
        this.newsService = newsService;
        this.newsAiEnrichmentService = newsAiEnrichmentService;
        this.portfolioAssistantContextService = portfolioAssistantContextService;
        this.alphaVantageAssetService = alphaVantageAssetService;
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

    @GetMapping("/stocks/{symbol}/candles")
        @Operation(
            summary = "Get stock candlestick history",
            description = "Fetches OHLCV candlestick history for a symbol, used to render a TradingView-style price chart. "
                    + "Uses Yahoo Finance's free chart API (no key required); falls back to Finnhub if configured and Yahoo fails.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Candle data fetched successfully"),
            @ApiResponse(responseCode = "502", description = "External market API error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
        public CandleResponse getStockCandles(
            @Parameter(description = "Stock ticker symbol", example = "AAPL", required = true)
            @PathVariable String symbol,
            @Parameter(description = "Candle resolution: 1,5,15,30,60,D,W,M", example = "D")
            @RequestParam(required = false, defaultValue = "D") String resolution,
            @Parameter(description = "How many days of history to fetch", example = "30")
            @RequestParam(required = false, defaultValue = "30") int days) {
        String interval;
        String range;
        if ("1".equals(resolution) || "5".equals(resolution) || "15".equals(resolution) || "30".equals(resolution) || "60".equals(resolution)) {
            interval = resolution + "m";
            if ("60".equals(resolution)) {
                interval = "60m";
            }
            range = days <= 1 ? "1d" : "5d";
        } else if ("W".equalsIgnoreCase(resolution)) {
            interval = "1wk";
            range = "2y";
        } else if ("M".equalsIgnoreCase(resolution)) {
            interval = "1mo";
            range = "5y";
        } else {
            interval = "1d";
            range = days <= 30 ? "1mo" : days <= 182 ? "6mo" : days <= 365 ? "1y" : "2y";
        }

        try {
            return yahooFinanceService.getCandles(symbol, range, interval);
        } catch (Exception yahooEx) {
            log.warn("Yahoo candle fetch failed for {}, trying Finnhub: {}", symbol, yahooEx.getMessage());
            return finnhubStockService.getCandles(symbol, resolution, days);
        }
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

    @GetMapping("/news/portfolio-brief")
        @Operation(
            summary = "Get AI-generated, portfolio-aware news brief",
            description = "Uses Groq's stronger model to explain how the latest market news relates to the user's holdings.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brief generated successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "External AI provider error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
    public NewsPortfolioBrief getPortfolioNewsBrief(
            @Parameter(description = "Optional portfolio ID to scope the brief to", example = "1")
            @RequestParam(required = false) Long portfolioId) {
        List<NewsArticle> articles = newsService.getCachedArticles();
        String portfolioContext;
        try {
            portfolioContext = portfolioAssistantContextService.build(portfolioId).llmContext();
        } catch (Exception e) {
            portfolioContext = "No portfolio context available.";
        }

        String brief;
        try {
            brief = newsAiEnrichmentService.generatePortfolioBrief(articles, portfolioContext);
        } catch (Exception e) {
            log.warn("Portfolio news brief generation failed: {}", e.getMessage());
            brief = "AI news brief is unavailable right now. Please configure GROQ_API_KEY or try again later.";
        }

        return new NewsPortfolioBrief(brief, "llama-3.3-70b-versatile", java.time.LocalDateTime.now());
    }

    @GetMapping("/assets/search")
        @Operation(
            summary = "Search tradable assets (stocks, ETFs, mutual funds)",
            description = "Uses Alpha Vantage's SYMBOL_SEARCH to look up instruments by name or ticker keyword, "
                    + "so ETFs and mutual funds (in addition to equities) can be selected when adding a holding.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "502", description = "External Alpha Vantage API error",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
        })
    public List<AssetSearchResult> searchAssets(
            @Parameter(description = "Keyword to search for, e.g. company name or ticker", example = "vanguard", required = true)
            @RequestParam String query) {
        return alphaVantageAssetService.searchSymbols(query);
    }
}
