package com.example.portfoliomanager.repository;

import com.example.portfoliomanager.domain.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByPortfolioId(Long portfolioId);
    List<Watchlist> findByPortfolioIdIn(List<Long> portfolioIds);
}
