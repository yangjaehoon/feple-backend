package com.feple.feple_backend.admin;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FestivalPageScraper {

    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{4})[.\\-/년]\\s*(\\d{1,2})[.\\-/월]\\s*(\\d{1,2})"
    );

    // SPA 껍데기로 판단하는 사이트별 기본 title
    private static final List<String> SPA_FALLBACK_TITLES = List.of(
        "NOL 티켓", "NOL 인터파크", "인터파크티켓", "인터파크 티켓",
        "예스24 티켓", "YES24", "멜론티켓", "MELON TICKET",
        "티켓링크", "하나티켓", "알티켓"
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

        log.debug("Jsoup scraped [{}] {} → title={}", source, url, title);

        return new ScrapedFestivalDto(title, description, location,
            dates[0], dates[1], imageUrl, url, source, null);
    }

    boolean isSpaOrEmpty(ScrapedFestivalDto r) {
        return (r.title().isBlank() || isSpaTitle(r.title()))
            && r.description().isBlank()
            && r.location().isBlank();
    }

    private String extractTitle(Document doc, String source) {
        for (String sel : new String[]{
            "meta[property=og:title]", "meta[name=twitter:title]"
        }) {
            String v = doc.select(sel).attr("content").trim();
            if (!v.isBlank() && !isSpaTitle(v)) return v;
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
                doc.select("h2.info_title").text()
            );
            case "melon" -> firstNonEmpty(
                doc.select(".subject_wrap .subject").text(),
                doc.select(".info_tit").text(),
                doc.select(".concert_tit").text()
            );
            default -> "";
        };
        if (!specific.isBlank()) return specific;

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
        return switch (source) {
            case "interpark" -> firstNonEmpty(
                doc.select(".goods_detail_info").text(),
                doc.select(".box_con_detail .con").text()
            );
            case "yes24" -> firstNonEmpty(doc.select(".goods_intro").text());
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

    private String firstNonEmpty(String... candidates) {
        for (String s : candidates) {
            if (s != null && !s.isBlank()) return s.trim();
        }
        return "";
    }
}
