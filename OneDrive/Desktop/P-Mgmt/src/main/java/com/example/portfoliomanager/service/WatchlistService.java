package com.example.portfoliomanager.service;

import com.example.portfoliomanager.domain.Watchlist;
import com.example.portfoliomanager.dto.ApiDtos.WatchlistRequest;
import com.example.portfoliomanager.dto.ApiDtos.WatchlistResponse;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.WatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class WatchlistService {

    private final WatchlistRepository repository;
    private final PortfolioService portfolioService;

    public WatchlistService(WatchlistRepository repository, PortfolioService portfolioService) {
        this.repository = repository;
        this.portfolioService = portfolioService;
    }

    @Transactional(readOnly = true)
    public List<WatchlistResponse> findAll(Long portfolioId) {
        List<Watchlist> entries;
        if (portfolioId != null) {
            portfolioService.getEntity(portfolioId); // validates ownership
            entries = repository.findByPortfolioId(portfolioId);
        } else {
            List<Long> userPortfolioIds = portfolioService.findAll().stream()
                    .map(com.example.portfoliomanager.dto.ApiDtos.PortfolioResponse::id).toList();
            if (userPortfolioIds.isEmpty()) {
                return java.util.List.of();
            }
            entries = repository.findByPortfolioIdIn(userPortfolioIds);
        }
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
        entry.setSymbol(request.symbol().trim().toUpperCase(Locale.ROOT));
        entry.setCompanyName(request.companyName());
        entry.setTargetPrice(request.targetPrice());
        entry.setPortfolio(portfolioService.getEntity(request.portfolioId()));
    }

    private WatchlistResponse toResponse(Watchlist entry) {
        return new WatchlistResponse(entry.getId(), entry.getSymbol(), entry.getCompanyName(),
                entry.getTargetPrice(), entry.getCurrentPrice(), entry.getLastPriceUpdate(),
                entry.getCreatedAt(), entry.getPortfolio().getId());
    }
}
