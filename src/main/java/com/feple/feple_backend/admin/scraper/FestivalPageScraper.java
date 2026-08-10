package com.feple.feple_backend.admin.scraper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FestivalPageScraper {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    // 대상 서버가 비정상적으로 큰 응답을 반환해도 메모리를 무제한 소비하지 않도록 상한을 둔다.
    private static final int MAX_RESPONSE_BYTES = 10 * 1024 * 1024;

    private static final List<String> SPA_FALLBACK_TITLES = List.of(
        "NOL 티켓", "NOL 인터파크", "인터파크티켓", "인터파크 티켓",
        "예스24 티켓", "YES24", "멜론티켓", "MELON TICKET",
        "티켓링크", "하나티켓", "알티켓"
    );

    private final CloseableHttpClient httpClient;

    public FestivalPageScraper(CloseableHttpClient safeScraperHttpClient) {
        this.httpClient = safeScraperHttpClient;
    }

    public ScrapedFestivalDto scrape(String url, String source) throws IOException {
        SsrfUrlValidator.validate(url);
        Document doc = fetchDocument(url);

        String title       = extractTitle(doc, source);
        String description = extractDescription(doc, source);
        String imageUrl    = extractImageUrl(doc);
        String location    = extractLocation(doc, source);
        String[] dates     = extractDates(doc, source);

        log.debug("Jsoup scraped [{}] {} → title={}", source, url, title);

        return new ScrapedFestivalDto(title, description, location,
            dates[0], dates[1], imageUrl, url, source, null);
    }

    private Document fetchDocument(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", USER_AGENT);
        request.setHeader("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8");
        request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        request.setHeader("Referer", "https://www.google.com");
        return httpClient.execute(request, response -> {
            int status = response.getCode();
            if (status < 200 || status >= 300) {
                throw new IOException("페이지 요청 실패: HTTP " + status);
            }
            HttpEntity entity = response.getEntity();
            byte[] body = entity == null ? new byte[0] : readBounded(entity.getContent());
            return Jsoup.parse(new ByteArrayInputStream(body), null, url);
        });
    }

    private static byte[] readBounded(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > MAX_RESPONSE_BYTES) {
                throw new IOException("응답 크기가 허용 범위를 초과했습니다.");
            }
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    boolean isSpaOrEmpty(ScrapedFestivalDto r) {
        return (r.title().isBlank() || isSpaTitle(r.title()))
            && r.description().isBlank()
            && r.location().isBlank();
    }

    private SiteScraperConfig strategy(String source) {
        return SiteScraperConfigs.forSource(source);
    }

    private String extractTitle(Document doc, String source) {
        for (String sel : new String[]{
            "meta[property=og:title]", "meta[name=twitter:title]"
        }) {
            String content = doc.select(sel).attr("content").trim();
            if (!content.isBlank() && !isSpaTitle(content)) return content;
        }
        for (String sel : strategy(source).titleSelectors()) {
            String content = doc.select(sel).text().trim();
            if (!content.isBlank()) return content;
        }
        String htmlTitle = doc.title().replaceAll("\\s*[|\\-–—].*", "").trim();
        return isSpaTitle(htmlTitle) ? "" : htmlTitle;
    }

    private String extractDescription(Document doc, String source) {
        for (String sel : new String[]{
            "meta[property=og:description]", "meta[name=twitter:description]", "meta[name=description]"
        }) {
            String content = doc.select(sel).attr("content").trim();
            if (!content.isBlank()) return content;
        }
        for (String sel : strategy(source).descriptionSelectors()) {
            String content = doc.select(sel).text().trim();
            if (!content.isBlank()) return content;
        }
        return "";
    }

    private String extractImageUrl(Document doc) {
        for (String sel : new String[]{
            "meta[property=og:image]", "meta[name=twitter:image]", "meta[name=twitter:image:src]"
        }) {
            String content = doc.select(sel).attr("content").trim();
            if (!content.isBlank()) return content;
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
