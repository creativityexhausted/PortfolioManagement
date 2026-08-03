package com.example.portfoliomanager.repository;

import com.example.portfoliomanager.domain.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.example.portfoliomanager.domain.AppUser;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByUser(AppUser user);
    Optional<Portfolio> findByIdAndUser(Long id, AppUser user);
}
