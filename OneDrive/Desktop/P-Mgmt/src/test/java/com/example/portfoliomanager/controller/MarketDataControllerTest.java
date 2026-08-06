package com.example.portfoliomanager.controller;

import com.example.portfoliomanager.chatbot.NewsAiEnrichmentService;
import com.example.portfoliomanager.chatbot.PortfolioAssistantContextService;
import com.example.portfoliomanager.dto.ApiDtos.AssetSearchResult;
import com.example.portfoliomanager.dto.ApiDtos.CandleResponse;
import com.example.portfoliomanager.dto.ApiDtos.NewsArticle;
import com.example.portfoliomanager.dto.ApiDtos.StockPriceResponse;
import com.example.portfoliomanager.service.AlphaVantageAssetService;
import com.example.portfoliomanager.service.FinnhubStockService;
import com.example.portfoliomanager.service.NewsService;
import com.example.portfoliomanager.service.YahooFinanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataControllerTest {

    @Mock
    private YahooFinanceService yahooFinanceService;

    @Mock
    private FinnhubStockService finnhubStockService;

    @Mock
    private NewsService newsService;

    @Mock
    private NewsAiEnrichmentService newsAiEnrichmentService;

    @Mock
    private PortfolioAssistantContextService portfolioAssistantContextService;

    @Mock
    private AlphaVantageAssetService alphaVantageAssetService;

    @InjectMocks
    private MarketDataController controller;

    @Test
    void getStockPriceDelegatesToYahoo() {
        StockPriceResponse expected = new StockPriceResponse("AAPL", "Apple", new BigDecimal("205.10"), "USD");
        when(yahooFinanceService.getQuote("AAPL")).thenReturn(expected);

        StockPriceResponse actual = controller.getStockPrice("AAPL");

        assertThat(actual).isEqualTo(expected);
        verify(yahooFinanceService).getQuote("AAPL");
    }

    @Test
    void getStockCandlesUsesYahooWhenAvailable() {
        CandleResponse expected = new CandleResponse("AAPL", "D", List.of());
        when(yahooFinanceService.getCandles("AAPL", "1mo", "1d")).thenReturn(expected);

        CandleResponse actual = controller.getStockCandles("AAPL", "D", 30);

        assertThat(actual).isEqualTo(expected);
        verify(yahooFinanceService).getCandles("AAPL", "1mo", "1d");
    }

    @Test
    void getNewsReturnsCachedArticles() {
        List<NewsArticle> expected = List.of(new NewsArticle(
                "Title",
                "Desc",
                "https://example.com",
                null,
                "2026-08-06T10:00:00Z",
                "Reuters",
                "NEUTRAL",
                "LOW",
                List.of(),
                List.of()));
        when(newsService.getCachedArticles()).thenReturn(expected);

        List<NewsArticle> actual = controller.getNews();

        assertThat(actual).isEqualTo(expected);
        verify(newsService).getCachedArticles();
    }

    @Test
    void searchAssetsDelegatesToAlphaVantageService() {
        List<AssetSearchResult> expected = List.of(new AssetSearchResult(
                "VOO",
                "Vanguard S&P 500 ETF",
                "ETF",
                "United States",
                "USD",
                0.85));
        when(alphaVantageAssetService.searchSymbols("vanguard")).thenReturn(expected);

        List<AssetSearchResult> actual = controller.searchAssets("vanguard");

        assertThat(actual).isEqualTo(expected);
        verify(alphaVantageAssetService).searchSymbols("vanguard");
    }
}
