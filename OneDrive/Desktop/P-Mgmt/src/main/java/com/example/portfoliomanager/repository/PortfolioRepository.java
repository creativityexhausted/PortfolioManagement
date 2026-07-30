package com.example.portfoliomanager.repository;

import com.example.portfoliomanager.domain.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
}
