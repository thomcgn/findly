package dev.thomcgn.findly.listing;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ListingFetchService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public ListingFetchResult fetch(String url) {
        try {
            String html = fetchHtml(url);
            String title = extractFirstMatch(html, Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
            String cleanedTitle = normalizeText(title);
            String description = extractMetaContent(html, "description");
            if (description == null || description.isBlank()) {
                description = extractTextSnippet(html, 180);
            }
            BigDecimal price = extractPrice(html);
            List<String> imageUrls = extractImageUrls(html);
            return new ListingFetchResult(
                    cleanedTitle == null || cleanedTitle.isBlank() ? "Unbenannter Artikel" : cleanedTitle,
                    description == null || description.isBlank() ? "Beschreibung konnte nicht automatisch geladen werden." : description,
                    price == null ? BigDecimal.ZERO : price,
                    "EUR",
                    imageUrls
            );
        } catch (Exception ex) {
            return new ListingFetchResult(
                    "Eingebettetes Angebot",
                    "Beschreibung konnte nicht automatisch geladen werden.",
                    BigDecimal.ZERO,
                    "EUR",
                    List.of()
            );
        }
    }

    private String fetchHtml(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (compatible; Findly/1.0; +https://findly.local)")
                .header("Accept-Language", "de-DE,de;q=0.9,en;q=0.8")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Unable to fetch URL: " + response.statusCode());
        }
        return response.body();
    }

    private BigDecimal extractPrice(String html) {
        String normalized = html
                .replace("&nbsp;", " ")
                .replace("&euro;", "€")
                .replace("&#x20AC;", "€");

        BigDecimal bestPrice = null;
        int bestPriority = Integer.MIN_VALUE;

        Pattern explicitPricePattern = Pattern.compile(
                "(?i)(?:neupreis|neu\\s+preis|preis\\s+waren|preis\\s+war|preis\\s+beträgt|price\\s+was|price\\s+is|price\\s*:|Preis\\s*:)[^\\d]{0,80}(\\d{1,5}(?:[\\.,]\\d{1,2})?)\\s*(?:€|EUR)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher explicitMatcher = explicitPricePattern.matcher(normalized);
        while (explicitMatcher.find()) {
            BigDecimal parsed = parsePriceValue(explicitMatcher.group(1));
            if (parsed != null && isReasonablePrice(parsed)) {
                if (bestPriority < 400) {
                    bestPrice = parsed;
                    bestPriority = 400;
                }
            }
        }

        Pattern genericPricePattern = Pattern.compile(
                "(?i)(?:preis|price|verkaufspreis|angebotspreis|kosten)[^\\d]{0,80}(\\d{1,5}(?:[\\.,]\\d{1,2})?)\\s*(?:€|EUR)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher genericMatcher = genericPricePattern.matcher(normalized);
        while (genericMatcher.find()) {
            BigDecimal parsed = parsePriceValue(genericMatcher.group(1));
            if (parsed != null && isReasonablePrice(parsed)) {
                if (bestPriority < 300) {
                    bestPrice = parsed;
                    bestPriority = 300;
                }
            }
        }

        Pattern simplePricePattern = Pattern.compile("(\\d{1,5}(?:[.,]\\d{1,2})?)\\s*(?:€|EUR)", Pattern.CASE_INSENSITIVE);
        Matcher simpleMatcher = simplePricePattern.matcher(normalized);
        while (simpleMatcher.find()) {
            BigDecimal parsed = parsePriceValue(simpleMatcher.group(1));
            if (parsed != null && isReasonablePrice(parsed)) {
                if (bestPriority < 100) {
                    bestPrice = parsed;
                    bestPriority = 100;
                }
            }
        }

        return bestPrice;
    }

    private boolean isReasonablePrice(BigDecimal value) {
        return value.compareTo(BigDecimal.valueOf(3)) >= 0 && value.compareTo(BigDecimal.valueOf(100000)) <= 0;
    }

    private BigDecimal parsePriceValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.trim().replace('.', ',');
        normalized = normalized.replace(".", "").replace(',', '.');
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String extractMetaContent(String html, String name) {
        Pattern pattern = Pattern.compile("<meta[^>]+name=\\\"" + name + "\\\"[^>]*content=\\\"([^\\\"]+)\\\"|<meta[^>]+content=\\\"([^\\\"]+)\\\"[^>]+name=\\\"" + name + "\\\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (value != null) {
                return value.replaceAll("\\s+", " ").trim();
            }
        }
        return null;
    }

    private String extractTextSnippet(String html, int maxLength) {
        String plain = html;
        plain = Pattern.compile("<script.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                .matcher(plain)
                .replaceAll(" ");
        plain = Pattern.compile("<style.*?</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                .matcher(plain)
                .replaceAll(" ");
        plain = Pattern.compile("<[^>]+>").matcher(plain).replaceAll(" ");
        plain = Pattern.compile("\\s+").matcher(plain).replaceAll(" ").trim();
        if (plain.length() <= maxLength) {
            return plain;
        }
        return plain.substring(0, maxLength).trim() + "...";
    }

    private List<String> extractImageUrls(String html) {
        Pattern pattern = Pattern.compile(
                "(?:og:image|twitter:image|contentUrl|content=|src=|data-src=)[^\\n]*?(https?://[^\"'\\s>]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(html);
        List<String> urls = new ArrayList<>();
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            if (!candidate.isBlank()) {
                urls.add(candidate);
            }
        }
        if (urls.isEmpty()) {
            Matcher imgMatcher = Pattern.compile("<img[^>]+(?:src|data-src|contentUrl)=\\\"([^\\\"]+)\\\"", Pattern.CASE_INSENSITIVE).matcher(html);
            while (imgMatcher.find()) {
                urls.add(imgMatcher.group(1));
            }
        }
        return urls.stream().distinct().limit(5).toList();
    }

    private String extractFirstMatch(String html, Pattern pattern) {
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
