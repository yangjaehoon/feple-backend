package com.feple.feple_backend.admin;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class WebScraperService {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{4})[.\\-/년]\\s*(\\d{1,2})[.\\-/월]\\s*(\\d{1,2})"
    );

    public ScrapedFestivalDto scrape(String url, String source) throws IOException {
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

        String warning = (title.isBlank() && description.isBlank())
            ? "이 사이트는 자바스크립트로 렌더링되어 일부 정보를 가져오지 못했을 수 있습니다. 직접 입력해주세요."
            : null;

        log.info("Scraped [{}] {} → title={}, dates={}/{}", source, url, title, dates[0], dates[1]);

        return new ScrapedFestivalDto(title, description, location,
            dates[0], dates[1], imageUrl, url, source, warning);
    }

    private String extractTitle(Document doc, String source) {
        for (String sel : new String[]{
            "meta[property=og:title]", "meta[name=twitter:title]"
        }) {
            String v = doc.select(sel).attr("content").trim();
            if (!v.isBlank()) return v;
        }

        String specific = switch (source) {
            case "interpark" -> firstNonEmpty(
                doc.select(".GoodsDetail .title").text(),
                doc.select(".box_concert_name span").text(),
                doc.select("h3.tit_goods").text(),
                doc.select(".goods_name").text()
            );
            case "yes24" -> firstNonEmpty(
                doc.select(".goods_name h2").text(),
                doc.select(".tit_goods").text(),
                doc.select("h2.info_title").text(),
                doc.select(".goods_name").text()
            );
            case "melon" -> firstNonEmpty(
                doc.select(".subject_wrap .subject").text(),
                doc.select(".info_tit").text(),
                doc.select(".concert_tit").text()
            );
            default -> "";
        };
        if (!specific.isBlank()) return specific;

        // HTML <title> fallback — strip site name suffix
        return doc.title().replaceAll("\\s*[|\\-–—].*", "").trim();
    }

    private String extractDescription(Document doc, String source) {
        for (String sel : new String[]{
            "meta[property=og:description]", "meta[name=twitter:description]", "meta[name=description]"
        }) {
            String v = doc.select(sel).attr("content").trim();
            if (!v.isBlank()) return v;
        }

        return switch (source) {
            case "interpark" -> firstNonEmpty(
                doc.select(".goods_detail_info").text(),
                doc.select(".box_con_detail .con").text()
            );
            case "yes24" -> firstNonEmpty(
                doc.select(".goods_intro").text()
            );
            default -> "";
        };
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
        String[] headers = switch (source) {
            case "interpark" -> new String[]{"장소", "공연장소", "행사장소"};
            case "yes24"     -> new String[]{"공연장소", "장소", "공연장"};
            case "melon"     -> new String[]{"장소", "공연장소"};
            default          -> new String[]{"장소", "공연장소", "공연장", "행사장소"};
        };
        return extractTableCell(doc, headers);
    }

    private String[] extractDates(Document doc, String source) {
        String[] headers = switch (source) {
            case "interpark" -> new String[]{"기간", "공연기간", "행사기간"};
            case "yes24"     -> new String[]{"공연기간", "기간", "행사기간"};
            case "melon"     -> new String[]{"기간", "공연기간"};
            default          -> new String[]{"기간", "공연기간", "행사기간"};
        };

        String raw = extractTableCell(doc, headers);
        if (!raw.isBlank()) {
            return parseDateRange(raw);
        }

        // JSON-LD structured data fallback
        for (Element script : doc.select("script[type=application/ld+json]")) {
            String json = script.html();
            String start = extractJsonValue(json, "startDate");
            String end   = extractJsonValue(json, "endDate");
            if (!start.isBlank()) {
                return new String[]{
                    normalizeDate(start),
                    normalizeDate(end.isBlank() ? start : end)
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
                    if (next != null && !next.text().isBlank()) {
                        return next.text().trim();
                    }
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

    private String firstNonEmpty(String... candidates) {
        for (String s : candidates) {
            if (s != null && !s.isBlank()) return s.trim();
        }
        return "";
    }
}
