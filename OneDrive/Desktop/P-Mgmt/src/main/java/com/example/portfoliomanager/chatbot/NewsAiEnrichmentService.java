package com.example.portfoliomanager.chatbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.portfoliomanager.dto.ApiDtos.NewsArticle;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Uses Groq's stronger model (e.g. llama-3.3-70b-versatile) to make the Market News section
 * context-aware: classifying sentiment/impact per article, extracting related tickers, and
 * producing a concise portfolio-aware "so what does this mean for me" brief.
 */
@Service
public class NewsAiEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(NewsAiEnrichmentService.class);
    private static final int MAX_ARTICLES_PER_BATCH = 12;

    private final GroqChatClient groqChatClient;
    private final GroqProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsAiEnrichmentService(GroqChatClient groqChatClient, GroqProperties properties) {
        this.groqChatClient = groqChatClient;
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.getGroq().getApiKey() != null && !properties.getGroq().getApiKey().isBlank();
    }

    /**
     * Classifies each article's sentiment/impact/summary/related tickers using Groq, in a single
     * batched call for efficiency. Falls back to returning the original articles unchanged if the
     * AI call fails or is not configured.
     */
    public List<NewsArticle> enrich(List<NewsArticle> articles) {
        if (!isConfigured() || articles == null || articles.isEmpty()) {
            return articles;
        }

        List<NewsArticle> batch = articles.size() > MAX_ARTICLES_PER_BATCH
                ? articles.subList(0, MAX_ARTICLES_PER_BATCH)
                : articles;

        try {
            String systemPrompt = """
                    You are a financial news analyst embedded in a stock portfolio terminal.
                    You will receive a JSON array of news articles (each with an "index", "title", "description", "source").
                    For EACH article, determine:
                    1) "sentiment": one of BULLISH, BEARISH, NEUTRAL - the likely market sentiment implied by the news.
                    2) "impact": one of HIGH, MEDIUM, LOW - how significant this news is for markets/investors.
                    3) "aiSummary": an array of 2-3 short, concrete bullet points (plain text, no markdown) summarizing the key
                       takeaways an investor needs to know.
                    4) "relatedSymbols": an array of 0-5 stock ticker symbols (e.g. "AAPL", "TSLA") explicitly or clearly
                       implied by the article. Use standard US/NSE tickers where possible. Return an empty array if unsure.

                    Respond with ONLY a valid JSON array, no markdown, no commentary, in this exact shape:
                    [{"index":0,"sentiment":"BULLISH","impact":"HIGH","aiSummary":["...","..."],"relatedSymbols":["NVDA"]}, ...]
                    The array must contain exactly one object per input article, in the same order, matching "index".
                    """;

            List<Map<String, Object>> payload = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                NewsArticle a = batch.get(i);
                payload.add(Map.of(
                        "index", i,
                        "title", a.title() == null ? "" : a.title(),
                        "description", a.description() == null ? "" : a.description(),
                        "source", a.source() == null ? "" : a.source()
                ));
            }

            String userPrompt = "Articles:\n" + objectMapper.writeValueAsString(payload);

            String raw = groqChatClient.chat(systemPrompt, userPrompt, properties.getGroq().getNewsModel());
            String jsonOnly = extractJsonArray(raw);

            List<EnrichmentResult> results = objectMapper.readValue(jsonOnly, new TypeReference<>() {
            });

            Map<Integer, EnrichmentResult> byIndex = results.stream()
                    .collect(Collectors.toMap(EnrichmentResult::index, r -> r, (a, b) -> a));

            List<NewsArticle> enriched = new ArrayList<>();
            for (int i = 0; i < batch.size(); i++) {
                NewsArticle original = batch.get(i);
                EnrichmentResult r = byIndex.get(i);
                if (r == null) {
                    enriched.add(original);
                    continue;
                }
                enriched.add(new NewsArticle(
                        original.title(),
                        original.description(),
                        original.url(),
                        original.urlToImage(),
                        original.publishedAt(),
                        original.source(),
                        normalizeSentiment(r.sentiment()),
                        normalizeImpact(r.impact()),
                        r.aiSummary() == null ? List.of() : r.aiSummary(),
                        r.relatedSymbols() == null ? List.of() : r.relatedSymbols()
                ));
            }

            // Append any remaining articles beyond the batch limit, unchanged.
            if (articles.size() > batch.size()) {
                enriched.addAll(articles.subList(batch.size(), articles.size()));
            }

            return enriched;
        } catch (Exception e) {
            log.warn("News AI enrichment failed, returning raw articles: {}", e.getMessage());
            return articles;
        }
    }

    /**
     * Generates a short, portfolio-aware narrative brief explaining how the current news set
     * relates to the user's actual holdings, using Groq's stronger model.
     */
    public String generatePortfolioBrief(List<NewsArticle> articles, String portfolioContext) {
        if (!isConfigured()) {
            throw new IllegalStateException("GROQ_API_KEY is not configured");
        }

        String systemPrompt = """
                You are a portfolio-aware financial news analyst.
                Rules:
                1) Use only the provided news headlines and portfolio context.
                2) Explain, in 3-5 concise sentences, how the current news landscape could relate to the user's
                   actual holdings (mention specific tickers/sectors from the portfolio context when relevant).
                3) Never give explicit buy/sell instructions; frame everything as factual observation or risk awareness.
                4) If no news relates to the portfolio, say so plainly and briefly summarize the broader market mood.
                5) Be concise and avoid repeating the raw headlines verbatim.
                """;

        String headlines = articles == null ? "" : articles.stream()
                .limit(15)
                .map(a -> "- " + a.title() + " (" + (a.source() == null ? "unknown source" : a.source()) + ")")
                .collect(Collectors.joining("\n"));

        String userPrompt = """
                Portfolio context:
                %s

                Latest market headlines:
                %s
                """.formatted(portfolioContext == null ? "No portfolio context available." : portfolioContext, headlines);

        return groqChatClient.chat(systemPrompt, userPrompt, properties.getGroq().getNewsModel());
    }

    private String extractJsonArray(String raw) {
        if (raw == null) return "[]";
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start == -1 || end == -1 || end < start) {
            return "[]";
        }
        return raw.substring(start, end + 1);
    }

    private String normalizeSentiment(String value) {
        if (value == null) return "NEUTRAL";
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "BULLISH", "POSITIVE", "POS" -> "BULLISH";
            case "BEARISH", "NEGATIVE", "NEG" -> "BEARISH";
            default -> "NEUTRAL";
        };
    }

    private String normalizeImpact(String value) {
        if (value == null) return "LOW";
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "HIGH" -> "HIGH";
            case "MEDIUM", "MED" -> "MEDIUM";
            default -> "LOW";
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EnrichmentResult(
            int index,
            String sentiment,
            String impact,
            List<String> aiSummary,
            List<String> relatedSymbols
    ) {
    }
}
