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
            @Value("${news.api.base-url:https://newsapi.org}") String baseUrl,
            @Value("${news.api.key:}") String apiKey,
            @Value("${news.api.query:stock market OR finance}") String query) {
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
                            .path("/v2/everything")
                            .queryParam("q", query)
                            .queryParam("language", "en")
                            .queryParam("sortBy", "publishedAt")
                            .queryParam("pageSize", 20)
                            .queryParam("apiKey", apiKey)
                            .build())
                    .retrieve()
                    .body(NewsApiResponse.class);

            List<NewsArticle> articles = response == null || response.articles() == null
                    ? List.of()
                    : response.articles().stream().map(this::toArticle).toList();
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
        String source = article.source() == null ? null : article.source().name();
        return new NewsArticle(article.title(), article.description(), article.url(),
                article.urlToImage(), article.publishedAt(), source);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NewsApiResponse(List<ArticlePayload> articles) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArticlePayload(
            SourcePayload source,
            String title,
            String description,
            String url,
            String urlToImage,
            String publishedAt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SourcePayload(String name) {
    }
}
