package com.example.portfoliomanager.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.portfoliomanager.domain.Holding;
import com.example.portfoliomanager.dto.ApiDtos.HoldingRequest;
import com.example.portfoliomanager.dto.ApiDtos.HoldingResponse;
import com.example.portfoliomanager.exception.ExternalApiException;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.HoldingRepository;
import com.example.portfoliomanager.service.PriceResolutionService.PriceLookupResult;

@Service
@Transactional
public class HoldingService {

    private static final Logger log = LoggerFactory.getLogger(HoldingService.class);

    private final HoldingRepository repository;
    private final PortfolioService portfolioService;
    private final PriceResolutionService priceResolutionService;
    private final com.example.portfoliomanager.repository.TransactionRepository transactionRepository;

    public HoldingService(
            HoldingRepository repository,
            PortfolioService portfolioService,
            PriceResolutionService priceResolutionService,
            com.example.portfoliomanager.repository.TransactionRepository transactionRepository) {
        this.repository = repository;
        this.portfolioService = portfolioService;
        this.priceResolutionService = priceResolutionService;
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
        return holdings.stream().map(h -> toResponse(h, null, List.of())).toList();
    }

    @Transactional(readOnly = true)
    public HoldingResponse findById(Long id) {
        return toResponse(getEntity(id), null, List.of());
    }

    public HoldingResponse create(HoldingRequest request) {
        String symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        Long portfolioId = request.portfolioId();
        List<String> notes = new ArrayList<>();
        String priceSource = null;

        // Resolve the historical purchase-batch price once (Finnhub -> Alpha Vantage -> Yahoo),
        // unless the user supplied one manually.
        BigDecimal newBatchPrice = request.averagePurchasePrice();
        if (newBatchPrice == null || newBatchPrice.compareTo(BigDecimal.ZERO) <= 0) {
            PriceLookupResult histResult = priceResolutionService.resolveHistoricalPrice(symbol, request.purchaseDate());
            notes.addAll(histResult.notes());
            if (histResult.found()) {
                newBatchPrice = histResult.quote().price();
                priceSource = histResult.source();
            }
        } else {
            notes.add("Used manually entered purchase price for " + symbol + ".");
        }

        if (newBatchPrice == null || newBatchPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExternalApiException(
                    "Could not automatically fetch a historical price for " + symbol + " on " + request.purchaseDate()
                            + " from Finnhub, Alpha Vantage, or Yahoo Finance. This can happen for mutual funds or "
                            + "thinly-traded tickers. Please enter the Avg Purchase Price manually.", null);
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

            // Update live/current price (Finnhub -> Alpha Vantage -> Yahoo)
            PriceLookupResult liveResult = priceResolutionService.resolveLivePrice(symbol);
            notes.addAll(liveResult.notes());
            if (liveResult.found()) {
                existing.setCurrentPrice(liveResult.quote().price());
                existing.setLastPriceUpdate(LocalDateTime.now());
                // Live price source takes priority in the response since it's the most recent lookup.
                priceSource = liveResult.source();
            }

            savedHolding = repository.save(existing);
        } else {
            // Creating brand new holding
            Holding holding = new Holding();
            String livePriceSource = applyNewHolding(holding, request, newBatchPrice, notes);
            if (livePriceSource != null) {
                priceSource = livePriceSource;
            }
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

        return toResponse(savedHolding, priceSource, notes);
    }

    public HoldingResponse update(Long id, HoldingRequest request) {
        Holding holding = getEntity(id);
        List<String> notes = new ArrayList<>();
        String priceSource = applyExistingHolding(holding, request, notes);
        return toResponse(repository.save(holding), priceSource, notes);
    }

    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private Holding getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Holding", id));
    }

    /**
     * Populates a brand-new Holding entity: the historical average purchase price is already
     * resolved by the caller and passed in as {@code batchPrice}; this method resolves the
     * current live price (Finnhub -> Alpha Vantage -> Yahoo) and fills in company name from
     * whichever provider responded. Returns the source of the live price for notifications.
     */
    private String applyNewHolding(Holding holding, HoldingRequest request, BigDecimal batchPrice, List<String> notes) {
        String symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        holding.setSymbol(symbol);
        holding.setQuantity(request.quantity());
        holding.setPortfolio(portfolioService.getEntity(request.portfolioId()));
        holding.setPurchaseDate(request.purchaseDate() != null ? request.purchaseDate() : java.time.LocalDate.now());
        holding.setAveragePurchasePrice(batchPrice);

        String companyName = request.companyName();

        PriceLookupResult liveResult = priceResolutionService.resolveLivePrice(symbol);
        notes.addAll(liveResult.notes());
        String liveSource = null;
        if (liveResult.found()) {
            holding.setCurrentPrice(liveResult.quote().price());
            holding.setLastPriceUpdate(LocalDateTime.now());
            liveSource = liveResult.source();
            if ((companyName == null || companyName.isBlank()) && liveResult.quote().companyName() != null) {
                companyName = liveResult.quote().companyName();
            }
        } else if (holding.getCurrentPrice() == null) {
            // As an absolute last resort (all 3 providers down for the live quote), fall back to
            // the historical/purchase price rather than leaving currentPrice null.
            holding.setCurrentPrice(batchPrice);
            notes.add("Live price unavailable from all providers for " + symbol + " — using purchase price as a placeholder.");
        }

        holding.setCompanyName((companyName != null && !companyName.isBlank()) ? companyName.trim() : symbol);
        return liveSource;
    }

    /**
     * Applies an update to an existing Holding (edit form), re-resolving the historical price
     * only if the user cleared/omitted it, and always refreshing the live price.
     * Returns the source of whichever provider most recently supplied a price, for notifications.
     */
    private String applyExistingHolding(Holding holding, HoldingRequest request, List<String> notes) {
        String symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        holding.setSymbol(symbol);
        holding.setQuantity(request.quantity());
        holding.setPortfolio(portfolioService.getEntity(request.portfolioId()));
        holding.setPurchaseDate(request.purchaseDate() != null ? request.purchaseDate() : java.time.LocalDate.now());

        String companyName = request.companyName();
        String priceSource = null;

        BigDecimal batchPrice = request.averagePurchasePrice();
        if (batchPrice != null && batchPrice.compareTo(BigDecimal.ZERO) > 0) {
            holding.setAveragePurchasePrice(batchPrice);
            notes.add("Used manually entered purchase price for " + symbol + ".");
        } else {
            PriceLookupResult histResult = priceResolutionService.resolveHistoricalPrice(symbol, request.purchaseDate());
            notes.addAll(histResult.notes());
            if (histResult.found()) {
                holding.setAveragePurchasePrice(histResult.quote().price());
                priceSource = histResult.source();
                if ((companyName == null || companyName.isBlank()) && histResult.quote().companyName() != null) {
                    companyName = histResult.quote().companyName();
                }
            } else {
                throw new ExternalApiException(
                        "Could not automatically fetch a historical price for " + symbol + " on " + request.purchaseDate()
                                + " from Finnhub, Alpha Vantage, or Yahoo Finance. Please enter the Avg Purchase Price manually.", null);
            }
        }

        PriceLookupResult liveResult = priceResolutionService.resolveLivePrice(symbol);
        notes.addAll(liveResult.notes());
        if (liveResult.found()) {
            holding.setCurrentPrice(liveResult.quote().price());
            holding.setLastPriceUpdate(LocalDateTime.now());
            priceSource = liveResult.source();
            if ((companyName == null || companyName.isBlank()) && liveResult.quote().companyName() != null) {
                companyName = liveResult.quote().companyName();
            }
        } else if (holding.getCurrentPrice() == null) {
            holding.setCurrentPrice(holding.getAveragePurchasePrice());
            notes.add("Live price unavailable from all providers for " + symbol + " — using purchase price as a placeholder.");
        }

        holding.setCompanyName((companyName != null && !companyName.isBlank()) ? companyName.trim() : symbol);
        return priceSource;
    }

    private HoldingResponse toResponse(Holding holding, String priceSource, List<String> notes) {
        return new HoldingResponse(holding.getId(), holding.getSymbol(), holding.getCompanyName(),
                holding.getQuantity(), holding.getAveragePurchasePrice(), holding.getCurrentPrice(),
                holding.getLastPriceUpdate(), holding.getPurchaseDate(), holding.getPortfolio().getId(),
                priceSource, notes == null ? List.of() : notes);
    }
}
