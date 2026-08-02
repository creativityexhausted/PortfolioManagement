package com.example.portfoliomanager.service;

import com.example.portfoliomanager.dto.ApiDtos.NewsArticle;
import com.example.portfoliomanager.exception.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class NewsService {

    private final RestClient restClient;
    private final String apiKey;
    private final String query;
    private final AtomicReference<List<NewsArticle>> cachedArticles = new AtomicReference<>(List.of());

    public NewsService(
            RestClient.Builder builder,
            @Value("${news.api.base-url:https://www.alphavantage.co}") String baseUrl,
            @Value("${news.api.key:}") String apiKey,
            @Value("${news.api.query:financial_markets}") String query) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.query = query;
    }

    public List<NewsArticle> getCachedArticles() {
        return cachedArticles.get();
    }

    public List<NewsArticle> refresh() {
        if (!StringUtils.hasText(apiKey)) {
            return cachedArticles.get();
        }
        try {
            NewsApiResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "NEWS_SENTIMENT")
                            .queryParam("topics", query)
                            .queryParam("limit", 15)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(NewsApiResponse.class);

            List<NewsArticle> articles = response == null || response.feed() == null
                    ? List.of()
                    : response.feed().stream().map(this::toArticle).toList();
            cachedArticles.set(articles);
            return articles;
        } catch (RuntimeException exception) {
            throw new ExternalApiException("News API request failed", exception);
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
                article.banner_image(), publishedAt, article.source());
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
