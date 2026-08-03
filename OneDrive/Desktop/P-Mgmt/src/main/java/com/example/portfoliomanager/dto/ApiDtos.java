package com.example.portfoliomanager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.portfoliomanager.domain.TransactionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ApiDtos {

    private ApiDtos() {
    }

    @Schema(description = "Payload to create or update an investment portfolio")
    public record PortfolioRequest(
            @Schema(description = "Portfolio display name", example = "Retirement Portfolio", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 100) String name,
            @Schema(description = "Optional portfolio description", example = "Long-term diversified holdings")
            @Size(max = 500) String description
    ) {
    }

    @Schema(description = "Portfolio resource details")
    public record PortfolioResponse(
            @Schema(description = "Portfolio identifier", example = "1")
            Long id,
            @Schema(description = "Portfolio display name", example = "Retirement Portfolio")
            String name,
            @Schema(description = "Portfolio description", example = "Long-term diversified holdings")
            String description,
            @Schema(description = "Creation timestamp", example = "2026-07-30T10:00:00")
            LocalDateTime createdAt,
            @Schema(description = "Last update timestamp", example = "2026-07-30T10:00:00")
            LocalDateTime updatedAt
    ) {
    }

    @Schema(description = "Payload to create or update a portfolio holding")
    public record HoldingRequest(
            @Schema(description = "Ticker symbol", example = "AAPL", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 20) String symbol,
            @Schema(description = "Company name", example = "Apple Inc.")
            @Size(max = 150) String companyName,
            @Schema(description = "Owned quantity", example = "12.5", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @Schema(description = "Average purchase price per share (optional, fetched from Finnhub live price if omitted)", example = "185.50")
            @DecimalMin("0.0") BigDecimal averagePurchasePrice,
            @Schema(description = "Purchase date of stock (optional, YYYY-MM-DD)", example = "2026-01-15")
            java.time.LocalDate purchaseDate,
            @Schema(description = "Portfolio ID to attach this holding", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Long portfolioId
    ) {
    }

    @Schema(description = "Holding resource details")
    public record HoldingResponse(
            @Schema(description = "Holding identifier", example = "1")
            Long id,
            @Schema(description = "Ticker symbol", example = "AAPL")
            String symbol,
            @Schema(description = "Company name", example = "Apple Inc.")
            String companyName,
            @Schema(description = "Owned quantity", example = "12.5")
            BigDecimal quantity,
            @Schema(description = "Average purchase price per share", example = "185.50")
            BigDecimal averagePurchasePrice,
            @Schema(description = "Latest market price", example = "197.42")
            BigDecimal currentPrice,
            @Schema(description = "Latest market price timestamp", example = "2026-07-30T10:15:00")
            LocalDateTime lastPriceUpdate,
            @Schema(description = "Purchase date of stock", example = "2026-01-15")
            java.time.LocalDate purchaseDate,
            @Schema(description = "Owning portfolio ID", example = "1")
            Long portfolioId
    ) {
    }

    @Schema(description = "Payload to create or update a transaction")
    public record TransactionRequest(
            @Schema(description = "Transaction type", example = "BUY", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull TransactionType type,
            @Schema(description = "Ticker symbol", example = "AAPL", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 20) String symbol,
            @Schema(description = "Transaction quantity", example = "12.5", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal quantity,
            @Schema(description = "Execution price per share", example = "185.50", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @DecimalMin("0.0") BigDecimal pricePerShare,
            @Schema(description = "Transaction date-time in ISO-8601", example = "2026-07-30T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull LocalDateTime transactionDate,
            @Schema(description = "Optional notes", example = "Initial position")
            @Size(max = 500) String notes,
            @Schema(description = "Portfolio ID to attach this transaction", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Long portfolioId
    ) {
    }

    @Schema(description = "Transaction resource details")
    public record TransactionResponse(
            @Schema(description = "Transaction identifier", example = "1")
            Long id,
            @Schema(description = "Transaction type", example = "BUY")
            TransactionType type,
            @Schema(description = "Ticker symbol", example = "AAPL")
            String symbol,
            @Schema(description = "Transaction quantity", example = "12.5")
            BigDecimal quantity,
            @Schema(description = "Execution price per share", example = "185.50")
            BigDecimal pricePerShare,
            @Schema(description = "Transaction date-time", example = "2026-07-30T10:00:00")
            LocalDateTime transactionDate,
            @Schema(description = "Transaction notes", example = "Initial position")
            String notes,
            @Schema(description = "Portfolio ID associated with transaction", example = "1")
            Long portfolioId
    ) {
    }

    @Schema(description = "Payload to create or update a watchlist entry")
    public record WatchlistRequest(
            @Schema(description = "Ticker symbol", example = "MSFT", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 20) String symbol,
            @Schema(description = "Company name", example = "Microsoft Corporation")
            @Size(max = 150) String companyName,
            @Schema(description = "Optional target price", example = "400")
            @DecimalMin("0.0") BigDecimal targetPrice,
            @Schema(description = "Portfolio ID for this watchlist entry", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Long portfolioId
    ) {
    }

    @Schema(description = "Watchlist resource details")
    public record WatchlistResponse(
            @Schema(description = "Watchlist entry identifier", example = "1")
            Long id,
            @Schema(description = "Ticker symbol", example = "MSFT")
            String symbol,
            @Schema(description = "Company name", example = "Microsoft Corporation")
            String companyName,
            @Schema(description = "Target price", example = "400")
            BigDecimal targetPrice,
            @Schema(description = "Latest market price", example = "412.35")
            BigDecimal currentPrice,
            @Schema(description = "Latest market price timestamp", example = "2026-07-30T10:15:00")
            LocalDateTime lastPriceUpdate,
            @Schema(description = "Entry creation timestamp", example = "2026-07-30T10:00:00")
            LocalDateTime createdAt,
            @Schema(description = "Owning portfolio ID", example = "1")
            Long portfolioId
    ) {
    }

    @Schema(description = "User registration request")
    public record RegisterRequest(
            @Schema(description = "Unique username", example = "demo-user", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(min = 3, max = 100) String username,
            @Schema(description = "Account password", example = "StrongPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(min = 8, max = 100) String password
    ) {
    }

    @Schema(description = "User authentication request")
    public record LoginRequest(
            @Schema(description = "Registered username", example = "demo-user", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String username,
            @Schema(description = "Account password", example = "StrongPass123!", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String password
    ) {
    }

    @Schema(description = "JWT authentication response")
    public record AuthResponse(
            @Schema(description = "Signed JWT token", example = "eyJhbGciOiJIUzM4NCJ9...")
            String token,
            @Schema(description = "Token type for Authorization header", example = "Bearer")
            String tokenType,
            @Schema(description = "Token expiration in milliseconds", example = "86400000")
            long expiresInMs
    ) {
    }

    @Schema(description = "A single OHLC candlestick point for charting")
    public record CandlePoint(
            @Schema(description = "Candle timestamp (epoch seconds)", example = "1732900800")
            long time,
            @Schema(description = "Open price", example = "195.10")
            BigDecimal open,
            @Schema(description = "High price", example = "198.40")
            BigDecimal high,
            @Schema(description = "Low price", example = "194.80")
            BigDecimal low,
            @Schema(description = "Close price", example = "197.42")
            BigDecimal close,
            @Schema(description = "Trading volume", example = "45213000")
            long volume
    ) {
    }

    @Schema(description = "Historical candlestick series for a symbol")
    public record CandleResponse(
            @Schema(description = "Ticker symbol", example = "AAPL")
            String symbol,
            @Schema(description = "Candle resolution", example = "D")
            String resolution,
            @Schema(description = "Ordered list of candle points, oldest first")
            java.util.List<CandlePoint> candles
    ) {
    }

    @Schema(description = "Latest stock quote details")
    public record StockPriceResponse(
            @Schema(description = "Ticker symbol", example = "AAPL")
            String symbol,
            @Schema(description = "Company name", example = "Apple Inc.")
            String companyName,
            @Schema(description = "Latest market price", example = "197.42")
            BigDecimal price,
            @Schema(description = "Quote currency", example = "USD")
            String currency
    ) {
    }

    @Schema(description = "Financial news item")
    public record NewsArticle(
            @Schema(description = "Headline", example = "Stocks rally on upbeat earnings")
            String title,
            @Schema(description = "Short description", example = "Major indices moved higher after strong earnings reports.")
            String description,
            @Schema(description = "Article URL", example = "https://news.example.com/article")
            String url,
            @Schema(description = "Preview image URL", example = "https://news.example.com/image.jpg")
            String urlToImage,
            @Schema(description = "Publish time", example = "2026-07-30T09:15:00Z")
            String publishedAt,
            @Schema(description = "Publisher name", example = "Reuters")
            String source,
            @Schema(description = "AI-classified sentiment", example = "BULLISH", allowableValues = {"BULLISH", "BEARISH", "NEUTRAL"})
            String sentiment,
            @Schema(description = "AI-classified market impact level", example = "HIGH", allowableValues = {"HIGH", "MEDIUM", "LOW"})
            String impact,
            @Schema(description = "AI-generated key takeaway bullet points")
            List<String> aiSummary,
            @Schema(description = "Tickers the AI identified as related to this article")
            List<String> relatedSymbols
    ) {
    }

    @Schema(description = "AI-generated, portfolio-aware news brief")
    public record NewsPortfolioBrief(
            @Schema(description = "Narrative brief explaining how current news affects the portfolio")
            String brief,
            @Schema(description = "Groq model used to generate the brief", example = "llama-3.3-70b-versatile")
            String model,
            @Schema(description = "Generation timestamp", example = "2026-08-03T10:15:00")
            LocalDateTime generatedAt
    ) {
    }

    @Schema(description = "Portfolio assistant chat request")
    public record ChatAssistantRequest(
            @Schema(description = "User message for the assistant", example = "Which holdings dropped the most this week?", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 1000) String message,
            @Schema(description = "Optional portfolio ID filter", example = "1")
            Long portfolioId
    ) {
    }

    @Schema(description = "Portfolio assistant chat response")
    public record ChatAssistantResponse(
            @Schema(description = "Assistant answer in plain language", example = "Your largest decline is in TSLA, down 4.2% from your average buy price.")
            String answer,
            @Schema(description = "Model used to generate the answer", example = "llama-3.1-8b-instant")
            String model,
            @Schema(description = "Response generation timestamp", example = "2026-07-31T10:15:00")
            LocalDateTime generatedAt
    ) {
    }
}
