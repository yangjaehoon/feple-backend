package com.feple.feple_backend.crawler.site;

import com.feple.feple_backend.crawler.CrawledFestivalData;
import com.feple.feple_backend.crawler.FestivalSiteCrawler;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
public class NolTicketCrawler implements FestivalSiteCrawler {

    private static final String SITE_NAME = "NOLTICKET";
    private static final String BASE_URL = "https://www.nolticket.com";
    // 놀티켓 페스티벌/공연 목록 페이지
    private static final String LIST_URL = "https://www.nolticket.com/ticket/list?genre=festival";
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})");

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

            // 상품 카드 셀렉터 (사이트 HTML 구조에 맞게 조정 필요)
            Elements items = doc.select(
                    ".ticket-item, .product-item, .lst-ticket li, " +
                    "[class*='ticket-card'], [class*='product-list'] li, " +
                    ".item-list li, ul.list-ticket > li"
            );

            log.info("[{}] {}개 항목 발견", SITE_NAME, items.size());

            for (Element item : items) {
                try {
                    String title   = extractText(item, ".tit, .title, h3, h4, [class*='name'], [class*='tit']");
                    String place   = extractText(item, ".place, .location, .venue, [class*='place'], [class*='venue']");
                    String dateStr = extractText(item, ".date, .period, .term, [class*='date'], [class*='period']");
                    String imgUrl  = extractImg(item);
                    String href    = extractHref(item);

                    if (title == null || title.isBlank()) continue;

                    String detailUrl = buildUrl(href);
                    LocalDate[] dates = parseDateRange(dateStr);

                    results.add(CrawledFestivalData.builder()
                            .title(title.trim())
                            .location(place)
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
        } catch (Exception e) {
            log.error("[{}] 크롤링 실패: {}", SITE_NAME, e.getMessage());
        }
        return results;
    }

    private String extractText(Element parent, String cssQuery) {
        for (String sel : cssQuery.split(",\\s*")) {
            Element el = parent.selectFirst(sel.trim());
            if (el != null && !el.text().isBlank()) return el.text();
        }
        return null;
    }

    private String extractImg(Element item) {
        Element img = item.selectFirst("img");
        if (img == null) return null;
        String src = img.attr("data-src");
        if (src.isBlank()) src = img.attr("data-lazy");
        if (src.isBlank()) src = img.attr("src");
        if (src.isBlank()) return null;
        return src.startsWith("http") ? src : BASE_URL + src;
    }

    private String extractHref(Element item) {
        Element a = item.selectFirst("a[href]");
        return a != null ? a.attr("href") : null;
    }

    private String buildUrl(String href) {
        if (href == null) return LIST_URL;
        return href.startsWith("http") ? href : BASE_URL + href;
    }

    private LocalDate[] parseDateRange(String raw) {
        LocalDate[] result = {null, null};
        if (raw == null) return result;
        List<LocalDate> dates = new ArrayList<>();
        Matcher m = DATE_PATTERN.matcher(raw);
        while (m.find()) {
            try {
                String normalized = String.format("%s.%02d.%02d",
                        m.group(1),
                        Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3)));
                dates.add(LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy.MM.dd")));
            } catch (DateTimeParseException ignored) {}
        }
        if (!dates.isEmpty()) result[0] = dates.get(0);
        result[1] = dates.size() >= 2 ? dates.get(1) : result[0];
        return result;
    }
}
