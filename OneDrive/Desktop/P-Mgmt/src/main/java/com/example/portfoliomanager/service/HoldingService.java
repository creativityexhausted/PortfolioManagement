package com.example.portfoliomanager.service;

import com.example.portfoliomanager.domain.Holding;
import com.example.portfoliomanager.dto.ApiDtos.HoldingRequest;
import com.example.portfoliomanager.dto.ApiDtos.HoldingResponse;
import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.HoldingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class HoldingService {

    private static final Logger log = LoggerFactory.getLogger(HoldingService.class);

    private final HoldingRepository repository;
    private final PortfolioService portfolioService;
    private final FinnhubStockService finnhubStockService;
    private final AlphaVantageAssetService alphaVantageAssetService;
    private final com.example.portfoliomanager.repository.TransactionRepository transactionRepository;

    public HoldingService(HoldingRepository repository, PortfolioService portfolioService, FinnhubStockService finnhubStockService, AlphaVantageAssetService alphaVantageAssetService, com.example.portfoliomanager.repository.TransactionRepository transactionRepository) {
        this.repository = repository;
        this.portfolioService = portfolioService;
        this.finnhubStockService = finnhubStockService;
        this.alphaVantageAssetService = alphaVantageAssetService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> findAll(Long portfolioId) {
        List<Holding> holdings;
        if (portfolioId != null) {
            portfolioService.getEntity(portfolioId); // validates ownership
            holdings = repository.findByPortfolioId(portfolioId);
        } else {
            List<Long> userPortfolioIds = portfolioService.findAll().stream()
                    .map(com.example.portfoliomanager.dto.ApiDtos.PortfolioResponse::id).toList();
            if (userPortfolioIds.isEmpty()) {
                return java.util.List.of();
            }
            holdings = repository.findByPortfolioIdIn(userPortfolioIds);
        }
        return holdings.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public HoldingResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public HoldingResponse create(HoldingRequest request) {
        String symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        Long portfolioId = request.portfolioId();

        // Fetch historical purchase price for this batch based on purchaseDate
        BigDecimal newBatchPrice = request.averagePurchasePrice();
        if (newBatchPrice == null || newBatchPrice.compareTo(BigDecimal.ZERO) <= 0) {
            try {
                StockPriceResponse quote = finnhubStockService.getHistoricalQuote(symbol, request.purchaseDate());
                if (quote != null && quote.price() != null) {
                    newBatchPrice = quote.price();
                }
            } catch (Exception e) {
                log.warn("Could not fetch historical quote for {} on {}: {}", symbol, request.purchaseDate(), e.getMessage());
            }
        }
        if (newBatchPrice == null || newBatchPrice.compareTo(BigDecimal.ZERO) <= 0) {
            // Finnhub does not support most mutual fund tickers - fall back to Alpha Vantage's
            // latest quote (best-effort approximation when a historical price isn't available).
            StockPriceResponse fallback = alphaVantageAssetService.getQuote(symbol);
            if (fallback != null && fallback.price() != null) {
                newBatchPrice = fallback.price();
            }
        }
        if (newBatchPrice == null || newBatchPrice.compareTo(BigDecimal.ZERO) <= 0) {
            newBatchPrice = BigDecimal.ZERO;
        }

        Holding savedHolding;

        // Check if portfolio already holds this stock symbol
        var existingOptional = repository.findByPortfolioIdAndSymbol(portfolioId, symbol);
        if (existingOptional.isPresent()) {
            Holding existing = existingOptional.get();

            BigDecimal q1 = existing.getQuantity();
            BigDecimal p1 = existing.getAveragePurchasePrice() != null ? existing.getAveragePurchasePrice() : BigDecimal.ZERO;

            BigDecimal q2 = request.quantity();
            BigDecimal p2 = newBatchPrice;

            BigDecimal totalQty = q1.add(q2);
            BigDecimal totalVal = q1.multiply(p1).add(q2.multiply(p2));
            BigDecimal weightedAvg = totalQty.compareTo(BigDecimal.ZERO) > 0
                    ? totalVal.divide(totalQty, 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            existing.setQuantity(totalQty);
            existing.setAveragePurchasePrice(weightedAvg);
            if (request.purchaseDate() != null && (existing.getPurchaseDate() == null || request.purchaseDate().isBefore(existing.getPurchaseDate()))) {
                existing.setPurchaseDate(request.purchaseDate());
            }

            // Update live price
            try {
                StockPriceResponse live = finnhubStockService.getQuote(symbol);
                if (live != null && live.price() != null) {
                    existing.setCurrentPrice(live.price());
                    existing.setLastPriceUpdate(LocalDateTime.now());
                } else {
                    throw new IllegalStateException("no price from finnhub");
                }
            } catch (Exception e) {
                StockPriceResponse fallback = alphaVantageAssetService.getQuote(symbol);
                if (fallback != null && fallback.price() != null) {
                    existing.setCurrentPrice(fallback.price());
                    existing.setLastPriceUpdate(LocalDateTime.now());
                }
            }

            savedHolding = repository.save(existing);
        } else {
            // Creating brand new holding
            Holding holding = new Holding();
            apply(holding, request, newBatchPrice, null);
            savedHolding = repository.save(holding);
        }

        // Record an automatic transaction for audit history
        try {
            var tx = new com.example.portfoliomanager.domain.Transaction();
            tx.setType(com.example.portfoliomanager.domain.TransactionType.BUY);
            tx.setSymbol(symbol);
            tx.setQuantity(request.quantity());
            tx.setPricePerShare(newBatchPrice);
            tx.setTransactionDate(request.purchaseDate() != null ? request.purchaseDate().atStartOfDay() : LocalDateTime.now());
            tx.setNotes("Holding addition batch");
            tx.setPortfolio(savedHolding.getPortfolio());
            transactionRepository.save(tx);
        } catch (Exception e) {
            log.warn("Could not save automatic transaction for holding batch: {}", e.getMessage());
        }

        return toResponse(savedHolding);
    }

    public HoldingResponse update(Long id, HoldingRequest request) {
        Holding holding = getEntity(id);
        apply(holding, request, request.averagePurchasePrice(), null);
        return toResponse(repository.save(holding));
    }

    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private Holding getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Holding", id));
    }

    private void apply(Holding holding, HoldingRequest request, BigDecimal batchPrice, StockPriceResponse preFetchedQuote) {
        String symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        holding.setSymbol(symbol);
        holding.setQuantity(request.quantity());
        holding.setPortfolio(portfolioService.getEntity(request.portfolioId()));
        holding.setPurchaseDate(request.purchaseDate() != null ? request.purchaseDate() : java.time.LocalDate.now());

        String companyName = request.companyName();

        // 1. Set Average Purchase Price (Historical)
        if (batchPrice != null && batchPrice.compareTo(BigDecimal.ZERO) > 0) {
            holding.setAveragePurchasePrice(batchPrice);
        } else {
            BigDecimal resolvedPrice = null;
            try {
                StockPriceResponse histQuote = finnhubStockService.getHistoricalQuote(symbol, request.purchaseDate());
                if (histQuote != null && histQuote.price() != null) {
                    resolvedPrice = histQuote.price();
                    if (companyName == null || companyName.isBlank()) {
                        companyName = histQuote.companyName();
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch historical quote for {} from Finnhub: {}", symbol, e.getMessage());
            }

            if (resolvedPrice == null) {
                // Finnhub does not cover most mutual fund tickers (returns 403/no data).
                // Fall back to Alpha Vantage's latest quote as a best-effort approximation.
                StockPriceResponse fallback = alphaVantageAssetService.getQuote(symbol);
                if (fallback != null && fallback.price() != null) {
                    resolvedPrice = fallback.price();
                }
            }

            if (resolvedPrice != null) {
                holding.setAveragePurchasePrice(resolvedPrice);
            } else {
                throw new com.example.portfoliomanager.exception.ExternalApiException(
                        "Could not automatically fetch a price for " + symbol + " from Finnhub or Alpha Vantage. "
                                + "This is common for mutual funds. Please enter the Avg Purchase Price manually.", null);
            }
        }

        // 2. Set Current Price (Live)
        try {
            StockPriceResponse liveQuote = preFetchedQuote != null ? preFetchedQuote : finnhubStockService.getQuote(symbol);
            if (liveQuote != null && liveQuote.price() != null) {
                holding.setCurrentPrice(liveQuote.price());
                holding.setLastPriceUpdate(LocalDateTime.now());
                if (companyName == null || companyName.isBlank()) {
                    companyName = liveQuote.companyName();
                }
            } else {
                throw new IllegalStateException("no live quote from finnhub");
            }
        } catch (Exception e) {
            log.warn("Could not fetch live quote for {} from Finnhub: {}", symbol, e.getMessage());
            StockPriceResponse fallback = alphaVantageAssetService.getQuote(symbol);
            if (fallback != null && fallback.price() != null) {
                holding.setCurrentPrice(fallback.price());
                holding.setLastPriceUpdate(LocalDateTime.now());
            } else if (holding.getCurrentPrice() == null) {
                holding.setCurrentPrice(holding.getAveragePurchasePrice());
            }
        }

        // 3. Set Company Name
        if (companyName != null && !companyName.isBlank()) {
            holding.setCompanyName(companyName.trim());
        } else {
            holding.setCompanyName(symbol);
        }
    }

    private HoldingResponse toResponse(Holding holding) {
        return new HoldingResponse(holding.getId(), holding.getSymbol(), holding.getCompanyName(),
                holding.getQuantity(), holding.getAveragePurchasePrice(), holding.getCurrentPrice(),
                holding.getLastPriceUpdate(), holding.getPurchaseDate(), holding.getPortfolio().getId());
    }
}
