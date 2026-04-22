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
public class MelonTicketCrawler implements FestivalSiteCrawler {

    private static final String SITE_NAME = "MELON";
    // 멜론 티켓 페스티벌 목록 (genreCode 009 = 페스티벌)
    private static final String LIST_URL =
            "https://tkglobal.melon.com/performance/index.htm?langCd=KO&genreCode=009";
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d{4})\\.(\\d{2})\\.(\\d{2})");

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

            // 멜론 티켓 공연 목록 ul > li 구조
            Elements items = doc.select("ul.lst_performance > li, .lst_concert > li, .list_wrap li");
            if (items.isEmpty()) {
                // fallback: 공통적인 상품 카드 셀렉터
                items = doc.select("[class*='item'][class*='perform'], [class*='concert'] li");
            }

            log.info("[{}] {}개 항목 발견", SITE_NAME, items.size());

            for (Element item : items) {
                try {
                    String title   = extractText(item, ".tit_performance, .tit, h3, h4");
                    String place   = extractText(item, ".place, .location, .venue, [class*='place']");
                    String dateStr = extractText(item, ".date, .period, .term, [class*='date']");
                    String imgUrl  = extractImg(item);
                    String href    = extractHref(item);

                    if (title == null || title.isBlank()) continue;

                    String detailUrl = href != null && href.startsWith("http")
                            ? href
                            : (href != null ? "https://tkglobal.melon.com" + href : LIST_URL);

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
        if (src.isBlank()) src = img.attr("src");
        return src.isBlank() ? null : src;
    }

    private String extractHref(Element item) {
        Element a = item.selectFirst("a[href]");
        return a != null ? a.attr("href") : null;
    }

    private LocalDate[] parseDateRange(String raw) {
        LocalDate[] result = {null, null};
        if (raw == null) return result;
        List<LocalDate> dates = new ArrayList<>();
        Matcher m = DATE_PATTERN.matcher(raw);
        while (m.find()) {
            try {
                dates.add(LocalDate.parse(
                        m.group(1) + "." + m.group(2) + "." + m.group(3),
                        DateTimeFormatter.ofPattern("yyyy.MM.dd")));
            } catch (DateTimeParseException ignored) {}
        }
        if (!dates.isEmpty()) result[0] = dates.get(0);
        if (dates.size() >= 2) result[1] = dates.get(1);
        else result[1] = result[0];
        return result;
    }
}
