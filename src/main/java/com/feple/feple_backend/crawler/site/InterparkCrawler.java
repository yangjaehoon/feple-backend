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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterparkCrawler implements FestivalSiteCrawler {

    private static final String SITE_NAME   = "INTERPARK";
    private static final String LIST_URL    = "https://nol.interpark.com/ticket";
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

            JsonNode root      = objectMapper.readTree(nextDataEl.html());
            JsonNode fallback  = root.path("props").path("pageProps").path("fallback");

            if (fallback.isMissingNode()) {
                log.warn("[{}] fallback 키를 찾지 못했습니다.", SITE_NAME);
                return results;
            }

            // fallback 은 SWR 캐시 맵: 각 값이 공연 배열일 수 있음
            // 모든 값을 순회하며 goodsCode 필드를 가진 배열을 찾음
            Iterator<Map.Entry<String, JsonNode>> fields = fallback.fields();
            while (fields.hasNext()) {
                JsonNode value = fields.next().getValue();
                if (!value.isArray() || value.isEmpty()) continue;
                if (!value.get(0).has("goodsCode") && !value.get(0).has("noticeId")) continue;

                for (JsonNode item : value) {
                    try {
                        String title     = item.path("title").asText(null);
                        String goodsCode = item.path("goodsCode").asText(null);
                        String place     = item.path("venueName").asText(null);
                        String dateStr   = item.path("openDateStr").asText(null); // "2026-05-15 20:00:00"
                        String imgUrl    = item.path("posterImageUrl").asText(null);

                        if (title == null || title.isBlank()) continue;

                        String detailUrl = goodsCode != null ? DETAIL_BASE + goodsCode : LIST_URL;

                        results.add(CrawledFestivalData.builder()
                                .title(title)
                                .location(place)
                                .startDate(parseDate(dateStr))
                                .endDate(parseDate(dateStr)) // 오픈 공지 데이터라 단일 날짜
                                .posterImageUrl(imgUrl)
                                .sourceUrl(detailUrl)
                                .sourceSite(SITE_NAME)
                                .build());
                    } catch (Exception e) {
                        log.debug("[{}] 항목 파싱 실패: {}", SITE_NAME, e.getMessage());
                    }
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
        // "2026-05-15 20:00:00" 또는 "2026-05-15T20:00:00"
        try { return LocalDate.parse(raw.substring(0, 10)); }
        catch (DateTimeParseException ignored) {}
        return null;
    }
}
