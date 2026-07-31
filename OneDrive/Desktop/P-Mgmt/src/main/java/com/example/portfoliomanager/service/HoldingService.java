package com.example.portfoliomanager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.portfoliomanager.domain.Holding;
import com.example.portfoliomanager.dto.ApiDtos.HoldingRequest;
import com.example.portfoliomanager.dto.ApiDtos.HoldingResponse;
import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.HoldingRepository;

@Service
@Transactional
public class HoldingService {

    private final HoldingRepository repository;
    private final PortfolioService portfolioService;
    private final YahooFinanceService yahooFinanceService;

    public HoldingService(
            HoldingRepository repository,
            PortfolioService portfolioService,
            YahooFinanceService yahooFinanceService) {
        this.repository = repository;
        this.portfolioService = portfolioService;
        this.yahooFinanceService = yahooFinanceService;
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> findAll(Long portfolioId) {
        List<Holding> holdings = portfolioId == null ? repository.findAll() : repository.findByPortfolioId(portfolioId);
        return holdings.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public HoldingResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public HoldingResponse create(HoldingRequest request) {
        Holding holding = new Holding();
        apply(holding, request);
        return toResponse(repository.save(holding));
    }

    public HoldingResponse update(Long id, HoldingRequest request) {
        Holding holding = getEntity(id);
        apply(holding, request);
        return toResponse(repository.save(holding));
    }

    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private Holding getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Holding", id));
    }

    private void apply(Holding holding, HoldingRequest request) {
        String symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        StockPriceResponse quote = yahooFinanceService.getQuote(symbol);

        holding.setSymbol(symbol);
        holding.setCompanyName(
                request.companyName() == null || request.companyName().isBlank()
                        ? quote.companyName()
                        : request.companyName().trim());
        holding.setQuantity(request.quantity());
        holding.setAveragePurchasePrice(request.averagePurchasePrice());
        holding.setCurrentPrice(quote.price());
        holding.setLastPriceUpdate(LocalDateTime.now());
        holding.setPortfolio(portfolioService.getEntity(request.portfolioId()));
    }

    private HoldingResponse toResponse(Holding holding) {
        return new HoldingResponse(holding.getId(), holding.getSymbol(), holding.getCompanyName(),
                holding.getQuantity(), holding.getAveragePurchasePrice(), holding.getCurrentPrice(),
                holding.getLastPriceUpdate(), holding.getPortfolio().getId());
    }
}
