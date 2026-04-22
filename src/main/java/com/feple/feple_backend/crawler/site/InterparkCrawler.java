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
    private static final String LIST_URL =
            "https://tickets.interpark.com/main?genreType=FES";

    private final ObjectMapper objectMapper;

    @Override
    public String getSiteName() {
        return SITE_NAME;
    }

    @Override
    public List<CrawledFestivalData> crawl() {
        List<CrawledFestivalData> results = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(LIST_URL)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                               "AppleWebKit/537.36 (KHTML, like Gecko) " +
                               "Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .timeout(20_000)
                    .get();

            // Next.js 앱은 __NEXT_DATA__ 스크립트에 초기 데이터를 포함
            Element nextDataEl = doc.selectFirst("script#__NEXT_DATA__");
            if (nextDataEl == null) {
                log.warn("[{}] __NEXT_DATA__ 스크립트를 찾지 못했습니다.", SITE_NAME);
                return results;
            }

            JsonNode root = objectMapper.readTree(nextDataEl.html());
            // 실제 경로는 페이지 구조에 따라 달라질 수 있음
            // 예: props → pageProps → genreData → goodsList
            JsonNode goodsList = root.path("props")
                                     .path("pageProps")
                                     .path("genreData")
                                     .path("goodsList");

            if (!goodsList.isArray()) {
                log.warn("[{}] goodsList 경로를 찾지 못했습니다. JSON 경로를 확인하세요.", SITE_NAME);
                return results;
            }

            for (JsonNode item : goodsList) {
                try {
                    String title      = item.path("goodsName").asText(null);
                    String goodsCode  = item.path("goodsCode").asText(null);
                    String place      = item.path("placeName").asText(null);
                    String startStr   = item.path("startDate").asText(null);
                    String endStr     = item.path("endDate").asText(null);
                    String imgUrl     = item.path("imgUrl").asText(null);

                    if (title == null || goodsCode == null) continue;

                    String detailUrl = "https://tickets.interpark.com/goods/" + goodsCode;

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

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // 인터파크 날짜 형식: "20250801" 또는 "2025.08.01"
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.length() == 8) {
            try {
                return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}
