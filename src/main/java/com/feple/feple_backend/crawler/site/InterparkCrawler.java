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

@Slf4j
@Component
@RequiredArgsConstructor
public class InterparkCrawler implements FestivalSiteCrawler {

    private static final String SITE_NAME = "INTERPARK";
    // 인터파크 → nol.interpark.com 으로 플랫폼 이전됨
    private static final String LIST_URL = "https://nol.interpark.com/ticket";
    private static final String DETAIL_BASE = "https://nol.interpark.com/ticket/goods/";

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
                    .header("Referer", "https://nol.interpark.com")
                    .timeout(20_000)
                    .get();

            Element nextDataEl = doc.selectFirst("script#__NEXT_DATA__");
            if (nextDataEl == null) {
                log.warn("[{}] __NEXT_DATA__ 스크립트를 찾지 못했습니다.", SITE_NAME);
                return results;
            }

            JsonNode root = objectMapper.readTree(nextDataEl.html());
            JsonNode pageProps = root.path("props").path("pageProps");

            // nol.interpark.com 에서 가능한 데이터 경로들을 순서대로 시도
            JsonNode goodsList = findGoodsList(pageProps);

            if (goodsList == null || !goodsList.isArray() || goodsList.isEmpty()) {
                log.warn("[{}] 상품 목록을 찾지 못했습니다. pageProps 키: {}", SITE_NAME, pageProps.fieldNames());
                return results;
            }

            for (JsonNode item : goodsList) {
                try {
                    String title     = firstText(item, "goodsName", "title", "name", "productName");
                    String goodsCode = firstText(item, "goodsCode", "productCode", "id", "goodsId");
                    String place     = firstText(item, "placeName", "venueName", "place", "location");
                    String startStr  = firstText(item, "startDate", "performStartDt", "openDate", "saleStartDate");
                    String endStr    = firstText(item, "endDate", "performEndDt", "closeDate", "saleEndDate");
                    String imgUrl    = firstText(item, "imgUrl", "posterUrl", "imageUrl", "thumbnailUrl");

                    if (title == null) continue;

                    String detailUrl = goodsCode != null ? DETAIL_BASE + goodsCode : LIST_URL;

                    results.add(CrawledFestivalData.builder()
                            .title(title)
                            .location(place)
                            .startDate(parseDate(startStr))
                            .endDate(parseDate(endStr))
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

    private JsonNode findGoodsList(JsonNode pageProps) {
        // 경로 후보들을 순서대로 탐색
        String[][] paths = {
            {"genreData", "goodsList"},
            {"rankingList"},
            {"goodsList"},
            {"productList"},
            {"data", "goodsList"},
            {"data", "list"},
            {"serverData", "list"},
            {"initialData", "goodsList"},
        };
        for (String[] path : paths) {
            JsonNode node = pageProps;
            for (String key : path) node = node.path(key);
            if (node.isArray() && !node.isEmpty()) return node;
        }
        return null;
    }

    private String firstText(JsonNode item, String... keys) {
        for (String key : keys) {
            JsonNode node = item.path(key);
            if (!node.isMissingNode() && !node.isNull() && !node.asText("").isBlank())
                return node.asText();
        }
        return null;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.length() == 8) {
            try { return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("yyyyMMdd")); }
            catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
