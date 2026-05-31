package com.feple.feple_backend.admin;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FestivalPageScraper {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{4})[.\\-/년]\\s*(\\d{1,2})[.\\-/월]\\s*(\\d{1,2})"
    );

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
        validateUrl(url);
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
        if (!raw.isBlank()) return parseDateRange(raw);

        for (Element script : doc.select("script[type=application/ld+json]")) {
            String json = script.html();
            String start = extractJsonValue(json, "startDate");
            String end   = extractJsonValue(json, "endDate");
            if (!start.isBlank()) {
                return new String[]{normalizeDate(start), normalizeDate(end.isBlank() ? start : end)};
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

    private String[] parseDateRange(String raw) {
        Matcher m = DATE_PATTERN.matcher(raw);
        String start = "", end = "";
        if (m.find()) {
            start = formatDate(m.group(1), m.group(2), m.group(3));
            if (m.find()) {
                end = formatDate(m.group(1), m.group(2), m.group(3));
            } else {
                end = start;
            }
        }
        return new String[]{start, end};
    }

    private String formatDate(String year, String month, String day) {
        try {
            return LocalDate.of(
                Integer.parseInt(year),
                Integer.parseInt(month.trim()),
                Integer.parseInt(day.trim())
            ).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeDate(String raw) {
        if (raw == null || raw.isBlank()) return "";
        if (raw.matches("\\d{4}-\\d{2}-\\d{2}.*")) return raw.substring(0, 10);
        return parseDateRange(raw)[0];
    }

    private String extractJsonValue(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private boolean isSpaTitle(String title) {
        if (title == null || title.isBlank()) return true;
        String lower = title.toLowerCase();
        return SPA_FALLBACK_TITLES.stream().anyMatch(t -> lower.contains(t.toLowerCase()));
    }

    // SSRF 방어: http/https만 허용, 루프백/사설/링크로컬 주소 차단
    private static void validateUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 URL입니다.");
        }
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("http/https URL만 허용됩니다.");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL에 호스트가 없습니다.");
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("호스트를 찾을 수 없습니다: " + host);
        }
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress() || addr.isAnyLocalAddress()) {
            throw new IllegalArgumentException("내부 네트워크 주소는 허용되지 않습니다.");
        }
    }
}
