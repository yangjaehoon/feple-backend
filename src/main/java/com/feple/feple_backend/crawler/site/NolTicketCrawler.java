package com.feple.feple_backend.crawler.site;

import com.feple.feple_backend.crawler.CrawledFestivalData;
import com.feple.feple_backend.crawler.FestivalSiteCrawler;
import com.feple.feple_backend.crawler.PlaywrightBrowser;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class NolTicketCrawler implements FestivalSiteCrawler {

    private static final String SITE_NAME = "NOLTICKET";
    private static final String LIST_URL  = "https://nol.yanolja.com";
    private static final String BASE_URL  = "https://nol.yanolja.com";
    // "2026.5.30 ~ 5.31" 또는 "2026.3.24 ~ 6.7"
    private static final Pattern FEATURES_DATE =
            Pattern.compile("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})(?:\\s*~\\s*(?:(\\d{4})\\.)?(\\d{1,2})\\.(\\d{1,2}))?");

    @Autowired(required = false)
    private PlaywrightBrowser playwrightBrowser;

    @Override
    public String getSiteName() { return SITE_NAME; }

    @Override
    @SuppressWarnings("unchecked")
    public List<CrawledFestivalData> crawl() {
        List<CrawledFestivalData> results = new ArrayList<>();
        if (playwrightBrowser == null) {
            log.warn("[{}] Playwright 비활성화 상태", SITE_NAME);
            return results;
        }
        try (PlaywrightBrowser.BrowserSession session = playwrightBrowser.openSession()) {
            Page page = session.newPage();
            try {
            page.navigate(LIST_URL);
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

            // DOM에서 직접 공연 카드 정보 추출 (id, title, location, features, image)
            List<Map<String, Object>> items = (List<Map<String, Object>>) page.evaluate("""
                () => {
                    const cards = document.querySelectorAll('[data-testid*="ticket"], [class*="TicketCard"], [class*="ConcertCard"], [class*="ticket-card"]');
                    if (cards.length > 0) {
                        return Array.from(cards).map(card => ({
                            title: card.querySelector('[class*="title"], h3, h4')?.innerText?.trim() ?? null,
                            location: card.querySelector('[class*="location"], [class*="place"], [class*="venue"]')?.innerText?.trim() ?? null,
                            features: card.querySelector('[class*="date"], [class*="period"], [class*="feature"]')?.innerText?.trim() ?? null,
                            image: card.querySelector('img')?.src ?? null,
                            href: card.querySelector('a')?.href ?? null
                        })).filter(i => i.title);
                    }
                    // fallback: 랭킹 리스트 항목
                    const listItems = document.querySelectorAll('li[class*="ranking"], li[class*="Ranking"], [class*="RankItem"]');
                    return Array.from(listItems).map(li => ({
                        title: li.querySelector('[class*="title"], [class*="name"]')?.innerText?.trim() ?? null,
                        location: li.querySelector('[class*="location"], [class*="place"]')?.innerText?.trim() ?? null,
                        features: li.querySelector('[class*="date"], [class*="period"]')?.innerText?.trim() ?? null,
                        image: li.querySelector('img')?.src ?? null,
                        href: li.querySelector('a')?.href ?? null
                    })).filter(i => i.title);
                }
            """);

            if (items == null || items.isEmpty()) {
                log.warn("[{}] DOM에서 항목을 찾지 못했습니다.", SITE_NAME);
                return results;
            }

            for (Map<String, Object> item : items) {
                try {
                    String title    = (String) item.get("title");
                    String location = (String) item.get("location");
                    String features = (String) item.get("features");
                    String imgUrl   = (String) item.get("image");
                    String href     = (String) item.get("href");

                    if (title == null || title.isBlank()) continue;

                    String detailUrl = (href != null && href.startsWith("http")) ? href : BASE_URL;
                    LocalDate[] dates = parseFeaturesDate(features);

                    results.add(CrawledFestivalData.builder()
                            .title(title)
                            .location(location)
                            .startDate(dates[0])
                            .endDate(dates[1])
                            .posterImageUrl(imgUrl)
                            .sourceUrl(detailUrl)
                            .sourceSite(SITE_NAME)
                            .build());
                } catch (Exception e) {
                    log.debug("[{}] 항목 파싱 실패: {}", SITE_NAME, e.getMessage());
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

    private LocalDate[] parseFeaturesDate(String raw) {
        LocalDate[] result = {null, null};
        if (raw == null || raw.isBlank()) return result;
        Matcher m = FEATURES_DATE.matcher(raw);
        if (m.find()) {
            try {
                result[0] = LocalDate.of(
                        Integer.parseInt(m.group(1)),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3)));
                int endYear  = m.group(4) != null ? Integer.parseInt(m.group(4))
                                                   : Integer.parseInt(m.group(1));
                if (m.group(5) != null) {
                    result[1] = LocalDate.of(endYear,
                            Integer.parseInt(m.group(5)),
                            Integer.parseInt(m.group(6)));
                } else {
                    result[1] = result[0];
                }
            } catch (Exception ignored) {}
        }
        return result;
    }
}
