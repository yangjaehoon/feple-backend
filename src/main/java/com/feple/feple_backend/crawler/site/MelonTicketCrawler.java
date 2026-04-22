package com.feple.feple_backend.crawler.site;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.crawler.CrawledFestivalData;
import com.feple.feple_backend.crawler.FestivalSiteCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class MelonTicketCrawler implements FestivalSiteCrawler {

    private static final String SITE_NAME = "MELON";
    private static final String LIST_URL = "https://www.ticketmelon.com";
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})");

    private final ObjectMapper objectMapper;

    @Override
    public String getSiteName() { return SITE_NAME; }

    @Override
    public List<CrawledFestivalData> crawl() {
        List<CrawledFestivalData> results = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(LIST_URL)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                               "AppleWebKit/537.36 (KHTML, like Gecko) " +
                               "Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .timeout(20_000)
                    .get();

            // Next.js / 유사 SSR 앱이면 __NEXT_DATA__ 시도
            Element nextDataEl = doc.selectFirst("script#__NEXT_DATA__");
            if (nextDataEl != null) {
                results.addAll(parseNextData(nextDataEl.html()));
                if (!results.isEmpty()) {
                    log.info("[{}] {}개 항목 수집 완료 (Next.js)", SITE_NAME, results.size());
                    return results;
                }
            }

            // fallback: HTML 셀렉터
            results.addAll(parseHtml(doc));
            log.info("[{}] {}개 항목 수집 완료 (HTML)", SITE_NAME, results.size());

        } catch (Exception e) {
            log.error("[{}] 크롤링 실패: {}", SITE_NAME, e.getMessage());
        }
        return results;
    }

    private List<CrawledFestivalData> parseNextData(String json) {
        List<CrawledFestivalData> results = new ArrayList<>();
        try {
            JsonNode pageProps = objectMapper.readTree(json).path("props").path("pageProps");
            JsonNode list = findList(pageProps);
            if (list == null || !list.isArray()) return results;

            for (JsonNode item : list) {
                String title    = firstText(item, "title", "name", "goodsName", "productName");
                String place    = firstText(item, "place", "venue", "placeName", "location");
                String startStr = firstText(item, "startDate", "openDate", "performStartDt");
                String endStr   = firstText(item, "endDate", "closeDate", "performEndDt");
                String imgUrl   = firstText(item, "imageUrl", "posterUrl", "imgUrl", "thumbnailUrl");
                String id       = firstText(item, "id", "goodsCode", "productId");

                if (title == null) continue;
                String detailUrl = id != null ? LIST_URL + "/goods/" + id : LIST_URL;

                results.add(CrawledFestivalData.builder()
                        .title(title)
                        .location(place)
                        .startDate(parseDateFlex(startStr))
                        .endDate(parseDateFlex(endStr))
                        .posterImageUrl(imgUrl)
                        .sourceUrl(detailUrl)
                        .sourceSite(SITE_NAME)
                        .build());
            }
        } catch (Exception e) {
            log.debug("[{}] Next.js JSON 파싱 실패: {}", SITE_NAME, e.getMessage());
        }
        return results;
    }

    private List<CrawledFestivalData> parseHtml(Document doc) {
        List<CrawledFestivalData> results = new ArrayList<>();
        Element items = doc.selectFirst(
                ".product-list, .ticket-list, .concert-list, [class*='product'] ul, [class*='ticket'] ul");
        if (items == null) return results;

        for (Element item : items.select("li, [class*='item'], [class*='card']")) {
            String title   = extractText(item, ".title, .tit, h3, h4");
            String place   = extractText(item, ".place, .location, .venue");
            String dateStr = extractText(item, ".date, .period, .term");
            String imgUrl  = extractImg(item);
            String href    = extractHref(item, LIST_URL);
            if (title == null) continue;
            LocalDate[] dates = parseDateRange(dateStr);
            results.add(CrawledFestivalData.builder()
                    .title(title.trim()).location(place)
                    .startDate(dates[0]).endDate(dates[1])
                    .posterImageUrl(imgUrl).sourceUrl(href)
                    .sourceSite(SITE_NAME).build());
        }
        return results;
    }

    // ── 공통 유틸 ──

    private JsonNode findList(JsonNode pageProps) {
        String[][] paths = {{"list"}, {"data", "list"}, {"goodsList"}, {"productList"}, {"items"}};
        for (String[] path : paths) {
            JsonNode n = pageProps;
            for (String k : path) n = n.path(k);
            if (n.isArray() && !n.isEmpty()) return n;
        }
        return null;
    }

    private String firstText(JsonNode item, String... keys) {
        for (String k : keys) {
            JsonNode n = item.path(k);
            if (!n.isMissingNode() && !n.isNull() && !n.asText("").isBlank()) return n.asText();
        }
        return null;
    }

    private String extractText(Element parent, String query) {
        for (String sel : query.split(",\\s*")) {
            Element el = parent.selectFirst(sel.trim());
            if (el != null && !el.text().isBlank()) return el.text();
        }
        return null;
    }

    private String extractImg(Element item) {
        Element img = item.selectFirst("img");
        if (img == null) return null;
        String src = img.attr("data-src");
        if (src.isBlank()) src = img.attr("src");
        return src.isBlank() ? null : src;
    }

    private String extractHref(Element item, String base) {
        Element a = item.selectFirst("a[href]");
        if (a == null) return base;
        String href = a.attr("href");
        return href.startsWith("http") ? href : base + href;
    }

    private LocalDate parseDateFlex(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.length() == 8) {
            try { return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("yyyyMMdd")); }
            catch (DateTimeParseException ignored) {}
        }
        LocalDate[] range = parseDateRange(raw);
        return range[0];
    }

    private LocalDate[] parseDateRange(String raw) {
        LocalDate[] result = {null, null};
        if (raw == null) return result;
        List<LocalDate> dates = new ArrayList<>();
        Matcher m = DATE_PATTERN.matcher(raw);
        while (m.find()) {
            try {
                dates.add(LocalDate.parse(
                        String.format("%s.%02d.%02d", m.group(1),
                                Integer.parseInt(m.group(2)),
                                Integer.parseInt(m.group(3))),
                        DateTimeFormatter.ofPattern("yyyy.MM.dd")));
            } catch (DateTimeParseException ignored) {}
        }
        if (!dates.isEmpty()) result[0] = dates.get(0);
        result[1] = dates.size() >= 2 ? dates.get(1) : result[0];
        return result;
    }
}
