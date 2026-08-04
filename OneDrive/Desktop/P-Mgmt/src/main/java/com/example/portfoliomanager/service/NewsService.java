package com.example.portfoliomanager.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.example.portfoliomanager.chatbot.NewsAiEnrichmentService;
import com.example.portfoliomanager.dto.ApiDtos.NewsArticle;
import com.example.portfoliomanager.exception.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String query;
    private final NewsAiEnrichmentService aiEnrichmentService;
    private final GNewsRssService gNewsRssService;
    private final AtomicReference<List<NewsArticle>> cachedArticles = new AtomicReference<>(List.of());
    private volatile boolean lastRefreshUsedFallback = false;

    public NewsService(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${news.api.base-url:https://www.alphavantage.co}") String baseUrl,
            @Value("${news.api.key:}") String apiKey,
            @Value("${news.api.query:financial_markets}") String query,
            NewsAiEnrichmentService aiEnrichmentService,
            GNewsRssService gNewsRssService) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.query = query;
        this.aiEnrichmentService = aiEnrichmentService;
        this.gNewsRssService = gNewsRssService;
    }

    public List<NewsArticle> getCachedArticles() {
        return cachedArticles.get();
    }

    /** True if the most recent refresh() had to fall back to Google News RSS. */
    public boolean isUsingFallbackSource() {
        return lastRefreshUsedFallback;
    }

    public List<NewsArticle> refresh() {
        if (!StringUtils.hasText(apiKey)) {
            return refreshFromFallback("Alpha Vantage news API key is not configured");
        }
        try {
            JsonNode root = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "NEWS_SENTIMENT")
                            .queryParam("topics", query)
                            .queryParam("limit", 15)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            // Alpha Vantage returns HTTP 200 even when rate-limited/erroring, using an
            // "Information"/"Note" field instead of the expected "feed" array. Without this
            // check the response would silently look like "0 articles found" instead of a
            // clear, actionable error.
            String notice = AlphaVantageErrorUtils.extractNotice(root);
            if (notice != null) {
                log.warn("Alpha Vantage news unavailable, falling back to Google News RSS: {}", AlphaVantageErrorUtils.summarize(notice));
                return refreshFromFallback("Alpha Vantage news feed unavailable: " + AlphaVantageErrorUtils.summarize(notice));
            }

            NewsApiResponse response = root == null ? null : objectMapper.treeToValue(root, NewsApiResponse.class);

            List<NewsArticle> articles = response == null || response.feed() == null
                    ? List.of()
                    : response.feed().stream().map(this::toArticle).toList();

            if (articles.isEmpty()) {
                log.info("Alpha Vantage returned zero news articles, falling back to Google News RSS");
                return refreshFromFallback("Alpha Vantage returned no articles");
            }

            // Enrich with AI-driven sentiment/impact/summary/related tickers using Groq's stronger model.
            List<NewsArticle> enriched = aiEnrichmentService.enrich(articles);
            cachedArticles.set(enriched);
            lastRefreshUsedFallback = false;
            return enriched;
        } catch (Exception exception) {
            log.warn("Alpha Vantage news request failed, falling back to Google News RSS: {}", exception.getMessage());
            return refreshFromFallback("Alpha Vantage news request failed: " + exception.getMessage());
        }
    }

    /**
     * Falls back to the free, keyless Google News RSS feed when Alpha Vantage's news
     * endpoint is unavailable (rate-limited, misconfigured, or erroring). AI enrichment is
     * still applied so the frontend gets the same shape/experience either way.
     * Only throws if the fallback itself also fails, so callers see one clear error rather
     * than two stacked ones.
     */
    private List<NewsArticle> refreshFromFallback(String primaryFailureReason) {
        try {
            List<NewsArticle> articles = gNewsRssService.fetchArticles(query, 15);
            if (articles.isEmpty()) {
                throw new ExternalApiException("News unavailable: " + primaryFailureReason + "; Google News RSS returned no results", null);
            }
            List<NewsArticle> enriched = aiEnrichmentService.enrich(articles);
            cachedArticles.set(enriched);
            lastRefreshUsedFallback = true;
            return enriched;
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (Exception fallbackException) {
            throw new ExternalApiException(
                    "News unavailable: " + primaryFailureReason + "; Google News RSS fallback also failed: " + fallbackException.getMessage(),
                    fallbackException);
        }
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    private NewsArticle toArticle(ArticlePayload article) {
        String publishedAt = article.time_published();
        // Alpha Vantage format: YYYYMMDDTHHMMSS -> Convert to standard ISO8601
        if (publishedAt != null && publishedAt.length() == 15) {
             publishedAt = publishedAt.substring(0, 4) + "-" + publishedAt.substring(4, 6) + "-" + 
                           publishedAt.substring(6, 8) + "T" + publishedAt.substring(9, 11) + ":" + 
                           publishedAt.substring(11, 13) + ":" + publishedAt.substring(13, 15) + "Z";
        }
        return new NewsArticle(article.title(), article.summary(), article.url(),
                article.banner_image(), publishedAt, article.source(),
                null, null, java.util.List.of(), java.util.List.of());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NewsApiResponse(List<ArticlePayload> feed) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArticlePayload(
            String source,
            String title,
            String summary,
            String url,
            String banner_image,
            String time_published
    ) {
    }
}
