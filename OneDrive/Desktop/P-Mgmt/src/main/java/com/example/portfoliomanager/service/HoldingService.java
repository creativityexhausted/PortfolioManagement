package com.example.portfoliomanager.service;

import com.example.portfoliomanager.domain.Holding;
import com.example.portfoliomanager.dto.ApiDtos.HoldingRequest;
import com.example.portfoliomanager.dto.ApiDtos.HoldingResponse;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class HoldingService {

    private final HoldingRepository repository;
    private final PortfolioService portfolioService;

    public HoldingService(HoldingRepository repository, PortfolioService portfolioService) {
        this.repository = repository;
        this.portfolioService = portfolioService;
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
        holding.setSymbol(request.symbol().trim().toUpperCase(Locale.ROOT));
        holding.setCompanyName(request.companyName());
        holding.setQuantity(request.quantity());
        holding.setAveragePurchasePrice(request.averagePurchasePrice());
        holding.setPortfolio(portfolioService.getEntity(request.portfolioId()));
    }

    private HoldingResponse toResponse(Holding holding) {
        return new HoldingResponse(holding.getId(), holding.getSymbol(), holding.getCompanyName(),
                holding.getQuantity(), holding.getAveragePurchasePrice(), holding.getCurrentPrice(),
                holding.getLastPriceUpdate(), holding.getPortfolio().getId());
    }
}
