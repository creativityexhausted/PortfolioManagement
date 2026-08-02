package com.example.portfoliomanager.service;

import com.example.portfoliomanager.domain.Portfolio;
import com.example.portfoliomanager.dto.ApiDtos.PortfolioRequest;
import com.example.portfoliomanager.dto.ApiDtos.PortfolioResponse;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PortfolioService {

    private final PortfolioRepository repository;

    public PortfolioService(PortfolioRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<PortfolioResponse> findAll() {
        List<Portfolio> list = repository.findAll();
        if (list.isEmpty()) {
            Portfolio main = new Portfolio();
            main.setName("Main Portfolio");
            main.setDescription("Primary Investment Account");
            list = List.of(repository.save(main));
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PortfolioResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    public PortfolioResponse create(PortfolioRequest request) {
        Portfolio portfolio = new Portfolio();
        apply(portfolio, request);
        return toResponse(repository.save(portfolio));
    }

    public PortfolioResponse update(Long id, PortfolioRequest request) {
        Portfolio portfolio = getEntity(id);
        apply(portfolio, request);
        return toResponse(repository.save(portfolio));
    }

    public void delete(Long id) {
        repository.delete(getEntity(id));
    }

    public Portfolio getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio", id));
    }

    private void apply(Portfolio portfolio, PortfolioRequest request) {
        portfolio.setName(request.name().trim());
        portfolio.setDescription(request.description());
    }

    private PortfolioResponse toResponse(Portfolio portfolio) {
        return new PortfolioResponse(portfolio.getId(), portfolio.getName(), portfolio.getDescription(),
                portfolio.getCreatedAt(), portfolio.getUpdatedAt());
    }
}
