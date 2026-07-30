package com.example.portfoliomanager.service;

import com.example.portfoliomanager.domain.Holding;
import com.example.portfoliomanager.domain.Watchlist;
import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.repository.HoldingRepository;
import com.example.portfoliomanager.repository.WatchlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MarketDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketDataScheduler.class);

    private final HoldingRepository holdingRepository;
    private final WatchlistRepository watchlistRepository;
    private final YahooFinanceService yahooFinanceService;
    private final NewsService newsService;

    public MarketDataScheduler(
            HoldingRepository holdingRepository,
            WatchlistRepository watchlistRepository,
            YahooFinanceService yahooFinanceService,
            NewsService newsService) {
        this.holdingRepository = holdingRepository;
        this.watchlistRepository = watchlistRepository;
        this.yahooFinanceService = yahooFinanceService;
        this.newsService = newsService;
    }

    @Scheduled(
            initialDelayString = "${market-data.schedule.initial-delay-ms:60000}",
            fixedDelayString = "${market-data.schedule.fixed-delay-ms:300000}")
    public void updateStockPrices() {
        holdingRepository.findAll().forEach(this::updateHolding);
        watchlistRepository.findAll().forEach(this::updateWatchlist);
    }

    @Scheduled(
            initialDelayString = "${news.schedule.initial-delay-ms:60000}",
            fixedDelayString = "${news.schedule.fixed-delay-ms:900000}")
    public void updateNews() {
        if (!newsService.isConfigured()) {
            log.debug("News update skipped because NEWS_API_KEY is not configured");
            return;
        }
        try {
            newsService.refresh();
        } catch (RuntimeException exception) {
            log.warn("Scheduled news refresh failed: {}", exception.getMessage());
        }
    }

    private void updateHolding(Holding holding) {
        try {
            StockPriceResponse quote = yahooFinanceService.getQuote(holding.getSymbol());
            holding.setCurrentPrice(quote.price());
            holding.setLastPriceUpdate(LocalDateTime.now());
            if (holding.getCompanyName() == null || holding.getCompanyName().isBlank()) {
                holding.setCompanyName(quote.companyName());
            }
            holdingRepository.save(holding);
        } catch (RuntimeException exception) {
            log.warn("Could not refresh holding {}: {}", holding.getSymbol(), exception.getMessage());
        }
    }

    private void updateWatchlist(Watchlist entry) {
        try {
            StockPriceResponse quote = yahooFinanceService.getQuote(entry.getSymbol());
            entry.setCurrentPrice(quote.price());
            entry.setLastPriceUpdate(LocalDateTime.now());
            if (entry.getCompanyName() == null || entry.getCompanyName().isBlank()) {
                entry.setCompanyName(quote.companyName());
            }
            watchlistRepository.save(entry);
        } catch (RuntimeException exception) {
            log.warn("Could not refresh watchlist symbol {}: {}", entry.getSymbol(), exception.getMessage());
        }
    }
}
