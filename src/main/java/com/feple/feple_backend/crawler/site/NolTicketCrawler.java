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
public class NolTicketCrawler implements FestivalSiteCrawler {

    private static final String SITE_NAME = "NOLTICKET";
    // 놀티켓 = 야놀자의 공연/전시 플랫폼 nol.yanolja.com
    private static final String BASE_URL  = "https://nol.yanolja.com";
    private static final String LIST_URL  = "https://nol.yanolja.com/concert";
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
                    .header("Referer", BASE_URL)
                    .timeout(20_000)
                    .get();

            // nol.yanolja.com 은 Next.js 앱
            Element nextDataEl = doc.selectFirst("script#__NEXT_DATA__");
            if (nextDataEl == null) {
                log.warn("[{}] __NEXT_DATA__ 스크립트를 찾지 못했습니다.", SITE_NAME);
                return results;
            }

            JsonNode root = objectMapper.readTree(nextDataEl.html());
            JsonNode pageProps = root.path("props").path("pageProps");

            JsonNode list = findList(pageProps);
            if (list == null || !list.isArray() || list.isEmpty()) {
                log.warn("[{}] 상품 목록을 찾지 못했습니다. pageProps 키: {}", SITE_NAME, pageProps.fieldNames());
                return results;
            }

            for (JsonNode item : list) {
                try {
                    String title    = firstText(item, "title", "goodsName", "productName", "name");
                    String place    = firstText(item, "venueName", "placeName", "place", "location");
                    String startStr = firstText(item, "startAt", "startDate", "performStartDt", "openDate");
                    String endStr   = firstText(item, "endAt", "endDate", "performEndDt", "closeDate");
                    String imgUrl   = firstText(item, "thumbnailUrl", "imageUrl", "posterUrl", "imgUrl");
                    String id       = firstText(item, "id", "goodsCode", "productId", "concertId");

                    if (title == null) continue;

                    String detailUrl = id != null ? BASE_URL + "/concert/" + id : LIST_URL;

                    results.add(CrawledFestivalData.builder()
                            .title(title)
                            .location(place)
                            .startDate(parseDateFlex(startStr))
                            .endDate(parseDateFlex(endStr))
                            .posterImageUrl(imgUrl)
                            .sourceUrl(detailUrl)
                            .sourceSite(SITE_NAME)
                            .build());
                } catch (Exception e) {
                    log.debug("[{}] 항목 파싱 실패: {}", SITE_NAME, e.getMessage());
                }
            }

            log.info("[{}] {}개 항목 수집 완료", SITE_NAME, results.size());
        } catch (Exception e) {
            log.error("[{}] 크롤링 실패: {}", SITE_NAME, e.getMessage());
        }
        return results;
    }

    private JsonNode findList(JsonNode pageProps) {
        String[][] paths = {
            {"list"},
            {"concertList"},
            {"data", "list"},
            {"serverData", "list"},
            {"initialData", "list"},
            {"goodsList"},
            {"productList"},
            {"items"},
        };
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

    private LocalDate parseDateFlex(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // ISO 형식: "2025-08-01T00:00:00"
        if (raw.length() >= 10 && raw.charAt(4) == '-') {
            try { return LocalDate.parse(raw.substring(0, 10)); }
            catch (DateTimeParseException ignored) {}
        }
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.length() == 8) {
            try { return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("yyyyMMdd")); }
            catch (DateTimeParseException ignored) {}
        }
        return parseDateRange(raw)[0];
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
