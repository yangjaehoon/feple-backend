package com.feple.feple_backend.admin;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FestivalPageScraper {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final List<String> SPA_FALLBACK_TITLES = List.of(
        "NOL 티켓", "NOL 인터파크", "인터파크티켓", "인터파크 티켓",
        "예스24 티켓", "YES24", "멜론티켓", "MELON TICKET",
        "티켓링크", "하나티켓", "알티켓"
    );

    private final Map<String, SiteScraperStrategy> strategies;

    public FestivalPageScraper(List<SiteScraperStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(SiteScraperStrategy::source, s -> s));
    }

    public ScrapedFestivalDto scrape(String url, String source) throws IOException {
        SsrfUrlValidator.validate(url);
        Document doc = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .referrer("https://www.google.com")
            .timeout(15_000)
            .get();

        String title       = extractTitle(doc, source);
        String description = extractDescription(doc, source);
        String imageUrl    = extractImageUrl(doc);
        String location    = extractLocation(doc, source);
        String[] dates     = extractDates(doc, source);

        log.debug("Jsoup scraped [{}] {} → title={}", source, url, title);

        return new ScrapedFestivalDto(title, description, location,
            dates[0], dates[1], imageUrl, url, source, null);
    }

    boolean isSpaOrEmpty(ScrapedFestivalDto r) {
        return (r.title().isBlank() || isSpaTitle(r.title()))
            && r.description().isBlank()
            && r.location().isBlank();
    }

    private SiteScraperStrategy strategy(String source) {
        return strategies.getOrDefault(source, DefaultSiteScraperStrategy.INSTANCE);
    }

    private String extractTitle(Document doc, String source) {
        for (String sel : new String[]{
            "meta[property=og:title]", "meta[name=twitter:title]"
        }) {
            String v = doc.select(sel).attr("content").trim();
            if (!v.isBlank() && !isSpaTitle(v)) return v;
        }
        for (String sel : strategy(source).titleSelectors()) {
            String v = doc.select(sel).text().trim();
            if (!v.isBlank()) return v;
        }
        String htmlTitle = doc.title().replaceAll("\\s*[|\\-–—].*", "").trim();
        return isSpaTitle(htmlTitle) ? "" : htmlTitle;
    }

    private String extractDescription(Document doc, String source) {
        for (String sel : new String[]{
            "meta[property=og:description]", "meta[name=twitter:description]", "meta[name=description]"
        }) {
            String v = doc.select(sel).attr("content").trim();
            if (!v.isBlank()) return v;
        }
        for (String sel : strategy(source).descriptionSelectors()) {
            String v = doc.select(sel).text().trim();
            if (!v.isBlank()) return v;
        }
        return "";
    }

    private String extractImageUrl(Document doc) {
        for (String sel : new String[]{
            "meta[property=og:image]", "meta[name=twitter:image]", "meta[name=twitter:image:src]"
        }) {
            String v = doc.select(sel).attr("content").trim();
            if (!v.isBlank()) return v;
        }
        return "";
    }

    private String extractLocation(Document doc, String source) {
        return extractTableCell(doc, strategy(source).locationHeaders());
    }

    private String[] extractDates(Document doc, String source) {
        String raw = extractTableCell(doc, strategy(source).dateHeaders());
        if (!raw.isBlank()) return FestivalDateParser.parseRange(raw);

        for (Element script : doc.select("script[type=application/ld+json]")) {
            String json  = script.html();
            String start = FestivalDateParser.extractJsonValue(json, "startDate");
            String end   = FestivalDateParser.extractJsonValue(json, "endDate");
            if (!start.isBlank()) {
                return new String[]{
                    FestivalDateParser.normalize(start),
                    FestivalDateParser.normalize(end.isBlank() ? start : end)
                };
            }
        }
        return new String[]{"", ""};
    }

    private String extractTableCell(Document doc, String[] headers) {
        for (String header : headers) {
            for (Element th : doc.select("th, dt")) {
                String text = th.text().trim();
                if (text.equals(header) || text.startsWith(header)) {
                    Element next = th.nextElementSibling();
                    if (next != null && !next.text().isBlank()) return next.text().trim();
                }
            }
        }
        return "";
    }

    private boolean isSpaTitle(String title) {
        if (title == null || title.isBlank()) return true;
        String lower = title.toLowerCase();
        return SPA_FALLBACK_TITLES.stream().anyMatch(t -> lower.contains(t.toLowerCase()));
    }
}
