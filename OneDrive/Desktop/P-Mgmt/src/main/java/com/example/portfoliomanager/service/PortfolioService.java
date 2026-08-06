package com.example.portfoliomanager.service;

import com.example.portfoliomanager.domain.Portfolio;
import com.example.portfoliomanager.dto.ApiDtos.PortfolioRequest;
import com.example.portfoliomanager.dto.ApiDtos.PortfolioResponse;
import com.example.portfoliomanager.exception.ResourceNotFoundException;
import com.example.portfoliomanager.repository.PortfolioRepository;
import com.example.portfoliomanager.domain.AppUser;
import com.example.portfoliomanager.repository.AppUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PortfolioService {

    private final PortfolioRepository repository;
    private final AppUserRepository appUserRepository;

    public PortfolioService(PortfolioRepository repository, AppUserRepository appUserRepository) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
    }

    private AppUser getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional
    public List<PortfolioResponse> findAll() {
        AppUser currentUser = getCurrentUser();
        List<Portfolio> list = repository.findByUser(currentUser);
        if (list.isEmpty()) {
            Portfolio main = new Portfolio();
            main.setName("Main Portfolio");
            main.setDescription("Primary Investment Account");
            main.setUser(currentUser);
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
        portfolio.setUser(getCurrentUser());
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
        return repository.findByIdAndUser(id, getCurrentUser())
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

    public com.example.portfoliomanager.dto.ApiDtos.QuantOptimizationResponse optimizePortfolio(Long id, com.example.portfoliomanager.dto.ApiDtos.QuantOptimizationRequest request) {
        // Ensure user owns the portfolio before allowing optimization
        getEntity(id);
        
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        
        org.springframework.http.HttpEntity<com.example.portfoliomanager.dto.ApiDtos.QuantOptimizationRequest> entity = new org.springframework.http.HttpEntity<>(request, headers);
        
        String pythonServiceUrl = "http://localhost:5001/api/optimize";
        org.springframework.http.ResponseEntity<com.example.portfoliomanager.dto.ApiDtos.QuantOptimizationResponse> response = restTemplate.postForEntity(pythonServiceUrl, entity, com.example.portfoliomanager.dto.ApiDtos.QuantOptimizationResponse.class);
        
        return response.getBody();
    }
}
