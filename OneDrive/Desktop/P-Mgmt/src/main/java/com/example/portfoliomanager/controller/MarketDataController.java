package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.dto.ApiDtos.NewsArticle;
import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.exception.ApiError;
import com.example.portfoliomanager.service.NewsService;
import com.example.portfoliomanager.service.YahooFinanceService;
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

    private final YahooFinanceService yahooFinanceService;
    private final NewsService newsService;

    public MarketDataController(YahooFinanceService yahooFinanceService, NewsService newsService) {
        this.yahooFinanceService = yahooFinanceService;
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
