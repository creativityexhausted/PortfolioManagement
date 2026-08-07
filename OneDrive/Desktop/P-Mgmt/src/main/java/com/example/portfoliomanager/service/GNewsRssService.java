package com.example.portfoliomanager.service;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.example.portfoliomanager.dto.ApiDtos.NewsArticle;
import com.example.portfoliomanager.exception.ExternalApiException;

/**
 * Free, keyless fallback news source used when Alpha Vantage's NEWS_SENTIMENT endpoint is
 * unavailable (typically due to its 25-requests/day rate limit). Consumes Google News' public
 * RSS feed, which has no API key and no meaningful rate limit for normal polling frequency.
 *
 * RSS item dates use RFC-1123 format (e.g. "Mon, 04 Aug 2026 09:00:00 GMT"), which we convert
 * to ISO-8601 to match the shape the frontend expects from the primary Alpha Vantage path.
 */
@Service
public class GNewsRssService {

    private static final Logger log = LoggerFactory.getLogger(GNewsRssService.class);
    private static final String RSS_BASE_URL = "https://news.google.com/rss/search";
    private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.RFC_1123_DATE_TIME;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /**
     * Fetches and parses up to {@code limit} articles matching the given query from Google
     * News RSS. Never returns null; throws {@link ExternalApiException} only if the feed
     * itself is unreachable/unparseable (network failure, not "no results").
     */
    public List<NewsArticle> fetchArticles(String query, int limit) {
        String q = (query == null || query.isBlank()) ? "stock market" : query;
        try {
            String encodedQuery = URLEncoder.encode(q, StandardCharsets.UTF_8);
            URI uri = URI.create(RSS_BASE_URL + "?q=" + encodedQuery + "&hl=en-US&gl=US&ceid=US:en");

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0 (compatible; AtlasPortfolioManager/1.0)")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new ExternalApiException("Google News RSS returned HTTP " + response.statusCode(), null);
            }

            return parseRss(response.body(), limit);
        } catch (ExternalApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Google News RSS fetch failed for query '{}': {}", q, exception.getMessage());
            throw new ExternalApiException("Google News RSS fetch failed: " + exception.getMessage(), exception);
        }
    }

    private List<NewsArticle> parseRss(InputStream xmlStream, int limit) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Harden against XXE - we don't need external entity/DTD resolution for a public RSS feed.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document document = factory.newDocumentBuilder().parse(xmlStream);
        NodeList items = document.getElementsByTagName("item");

        List<NewsArticle> articles = new ArrayList<>();
        int count = Math.min(items.getLength(), Math.max(limit, 0));
        for (int i = 0; i < count; i++) {
            if (!(items.item(i) instanceof Element item)) {
                continue;
            }
            String title = text(item, "title");
            String link = text(item, "link");
            String pubDate = text(item, "pubDate");
            String sourceName = sourceFromItem(item, title);
            String cleanTitle = stripSourceSuffix(title, sourceName);

            articles.add(new NewsArticle(
                    cleanTitle,
                    null, // Google News RSS descriptions are HTML snippets duplicating the title; omit rather than show noisy markup.
                    link,
                    null, // RSS <item> has no reliable image field
                    toIso8601(pubDate),
                    sourceName,
                    null, null,
                    List.of(),
                    List.of()
            ));
        }
        return articles;
    }

    private String sourceFromItem(Element item, String title) {
        NodeList sourceNodes = item.getElementsByTagName("source");
        if (sourceNodes.getLength() > 0) {
            String name = sourceNodes.item(0).getTextContent();
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
        }
        // Fallback: Google News titles are often "Headline - Publisher"
        if (title != null && title.contains(" - ")) {
            return title.substring(title.lastIndexOf(" - ") + 3).trim();
        }
        return "Google News";
    }

    private String stripSourceSuffix(String title, String sourceName) {
        if (title == null) {
            return null;
        }
        String suffix = " - " + sourceName;
        return title.endsWith(suffix) ? title.substring(0, title.length() - suffix.length()) : title;
    }

    private String toIso8601(String rfc1123Date) {
        if (rfc1123Date == null || rfc1123Date.isBlank()) {
            return null;
        }
        try {
            ZonedDateTime parsed = ZonedDateTime.parse(rfc1123Date, RFC_1123);
            return parsed.format(DateTimeFormatter.ISO_INSTANT);
        } catch (Exception exception) {
            return rfc1123Date;
        }
    }

    private String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) {
            return null;
        }
        String content = nodes.item(0).getTextContent();
        return content == null ? null : content.trim();
    }
}
