package com.feple.feple_backend.crawler.site;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.crawler.CrawledFestivalData;
import com.feple.feple_backend.crawler.FestivalSiteCrawler;
import com.feple.feple_backend.crawler.PlaywrightBrowser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final PlaywrightBrowser playwrightBrowser;
    private final ObjectMapper objectMapper;

    @Override
    public String getSiteName() { return SITE_NAME; }

    @Override
    public List<CrawledFestivalData> crawl() {
        List<CrawledFestivalData> results = new ArrayList<>();
        try (PlaywrightBrowser.BrowserSession session = playwrightBrowser.openSession()) {
            Page page = session.newPage();
            try {
                page.navigate(LIST_URL);
                page.waitForLoadState(LoadState.NETWORKIDLE);

                String json = (String) page.evaluate(
                        "() => { const el = document.getElementById('__NEXT_DATA__'); " +
                        "return el ? el.textContent : null; }");

                if (json == null) {
                    log.warn("[{}] __NEXT_DATA__ 없음", SITE_NAME);
                    return results;
                }

                JsonNode fallback = objectMapper.readTree(json)
                        .path("props").path("pageProps").path("fallback");

                Iterator<Map.Entry<String, JsonNode>> fields = fallback.fields();
                while (fields.hasNext()) {
                    JsonNode value = fields.next().getValue();
                    if (!value.isArray() || value.isEmpty()) continue;
                    JsonNode first = value.get(0);
                    if (!first.has("goodsCode") && !first.has("noticeId")) continue;

                    for (JsonNode item : value) {
                        try {
                            String title     = item.path("title").asText(null);
                            String goodsCode = item.path("goodsCode").asText(null);
                            String place     = item.path("venueName").asText(null);
                            String dateStr   = item.path("openDateStr").asText(null);
                            String imgUrl    = item.path("posterImageUrl").asText(null);
                            if (title == null || title.isBlank()) continue;
                            results.add(CrawledFestivalData.builder()
                                    .title(title).location(place)
                                    .startDate(parseDate(dateStr)).endDate(parseDate(dateStr))
                                    .posterImageUrl(imgUrl)
                                    .sourceUrl(goodsCode != null ? DETAIL_BASE + goodsCode : LIST_URL)
                                    .sourceSite(SITE_NAME).build());
                        } catch (Exception e) {
                            log.debug("[{}] 항목 파싱 실패: {}", SITE_NAME, e.getMessage());
                        }
                    }
                }
                log.info("[{}] {}개 항목 수집 완료", SITE_NAME, results.size());
            } finally {
                page.close();
            }
        } catch (Exception e) {
            log.error("[{}] 크롤링 실패: {}", SITE_NAME, e.getMessage());
        }
        return results;
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return LocalDate.parse(raw.substring(0, 10)); }
        catch (DateTimeParseException ignored) { return null; }
    }
}
