package com.example.portfoliomanager.repository;

import com.example.portfoliomanager.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByPortfolioId(Long portfolioId);
    List<Transaction> findByPortfolioIdIn(List<Long> portfolioIds);
}
