package com.example.portfoliomanager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.portfoliomanager.domain.Watchlist;
import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.dto.ApiDtos.WatchlistRequest;
import com.example.portfoliomanager.dto.ApiDtos.WatchlistResponse;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.WatchlistRepository;

@Service
@Transactional
public class WatchlistService {

    private final WatchlistRepository repository;
    private final PortfolioService portfolioService;
    private final YahooFinanceService yahooFinanceService;

    public WatchlistService(
            WatchlistRepository repository,
            PortfolioService portfolioService,
            YahooFinanceService yahooFinanceService) {
        this.repository = repository;
        this.portfolioService = portfolioService;
        this.yahooFinanceService = yahooFinanceService;
    }

    @Transactional(readOnly = true)
    public List<WatchlistResponse> findAll(Long portfolioId) {
        List<Watchlist> entries = portfolioId == null
                ? repository.findAll()
                : repository.findByPortfolioId(portfolioId);
        return entries.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WatchlistResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public WatchlistResponse create(WatchlistRequest request) {
        Watchlist entry = new Watchlist();
        apply(entry, request);
        return toResponse(repository.save(entry));
    }

    public WatchlistResponse update(Long id, WatchlistRequest request) {
        Watchlist entry = getEntity(id);
        apply(entry, request);
        return toResponse(repository.save(entry));
    }

    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    private Watchlist getEntity(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Watchlist entry", id));
    }

    private void apply(Watchlist entry, WatchlistRequest request) {
        String symbol = request.symbol().trim().toUpperCase(Locale.ROOT);
        StockPriceResponse quote = yahooFinanceService.getQuote(symbol);

        entry.setSymbol(symbol);
        entry.setCompanyName(
                request.companyName() == null || request.companyName().isBlank()
                        ? quote.companyName()
                        : request.companyName().trim());
        entry.setTargetPrice(request.targetPrice());
        entry.setCurrentPrice(quote.price());
        entry.setLastPriceUpdate(LocalDateTime.now());
        entry.setPortfolio(portfolioService.getEntity(request.portfolioId()));
    }

    private WatchlistResponse toResponse(Watchlist entry) {
        return new WatchlistResponse(entry.getId(), entry.getSymbol(), entry.getCompanyName(),
                entry.getTargetPrice(), entry.getCurrentPrice(), entry.getLastPriceUpdate(),
                entry.getCreatedAt(), entry.getPortfolio().getId());
    }
}
