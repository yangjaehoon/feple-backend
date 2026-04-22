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
@RequiredArgsConstructor
public class NolTicketCrawler implements FestivalSiteCrawler {

    private static final String SITE_NAME  = "NOLTICKET";
    private static final String BASE_URL   = "https://nol.yanolja.com";
    private static final String LIST_URL   = "https://nol.yanolja.com";
    // self.__next_f.push([숫자,"...JSON..."])
    private static final Pattern PUSH_PATTERN =
            Pattern.compile("self\\.__next_f\\.push\\(\\[\\d+,\"(.+?)\"\\]\\)\\s*;?",
                    Pattern.DOTALL);
    // "2026.5.30 ~ 5.31" 또는 "2026.3.24 ~ 6.7"
    private static final Pattern FEATURES_DATE =
            Pattern.compile("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2}).*?(?:(\\d{4})\\.)?(\\d{1,2})\\.(\\d{1,2})");

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

            // Next.js 스트리밍 방식: self.__next_f.push() 스크립트 태그들을 파싱
            Elements scripts = doc.select("script");
            int found = 0;
            for (Element script : scripts) {
                String src = script.html();
                if (!src.contains("self.__next_f.push")) continue;

                Matcher m = PUSH_PATTERN.matcher(src);
                while (m.find()) {
                    String escaped = m.group(1);
                    try {
                        // 이스케이프된 JSON 문자열을 파싱
                        String json = objectMapper.readValue("\"" + escaped + "\"", String.class);
                        List<CrawledFestivalData> extracted = extractFromRscChunk(json);
                        results.addAll(extracted);
                        found += extracted.size();
                    } catch (Exception ignored) {}
                }
            }

            log.info("[{}] {}개 항목 수집 완료", SITE_NAME, found);
        } catch (Exception e) {
            log.error("[{}] 크롤링 실패: {}", SITE_NAME, e.getMessage());
        }
        return results;
    }

    /**
     * RSC 청크 JSON에서 items 배열을 찾아 CrawledFestivalData 리스트로 변환
     */
    private List<CrawledFestivalData> extractFromRscChunk(String chunk) {
        List<CrawledFestivalData> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(chunk);
            findItemsRecursive(root, results, 0);
        } catch (Exception ignored) {}
        return results;
    }

    private void findItemsRecursive(JsonNode node, List<CrawledFestivalData> results, int depth) {
        if (depth > 8 || node == null) return;

        if (node.isArray() && !node.isEmpty()) {
            JsonNode first = node.get(0);
            // id, title, location, features 필드를 가진 배열이면 공연 목록으로 판단
            if (first.has("id") && first.has("title") && first.has("features")) {
                for (JsonNode item : node) {
                    try {
                        String id       = item.path("id").asText(null);
                        String title    = item.path("title").asText(null);
                        String location = item.path("location").asText(null);
                        String features = item.path("features").asText(null); // "2026.5.30 ~ 5.31"
                        String imgUrl   = item.path("image").path("url").asText(null);

                        if (title == null || title.isBlank()) continue;

                        String detailUrl = id != null ? BASE_URL + "/ticket/" + id : LIST_URL;
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
                    } catch (Exception ignored) {}
                }
                return;
            }
        }

        if (node.isObject()) {
            node.fields().forEachRemaining(entry ->
                    findItemsRecursive(entry.getValue(), results, depth + 1));
        } else if (node.isArray()) {
            node.forEach(child -> findItemsRecursive(child, results, depth + 1));
        }
    }

    /**
     * "2026.5.30 ~ 5.31" 또는 "2026.3.24 ~ 6.7" 형식 파싱
     */
    private LocalDate[] parseFeaturesDate(String raw) {
        LocalDate[] result = {null, null};
        if (raw == null || raw.isBlank()) return result;

        Matcher m = FEATURES_DATE.matcher(raw);
        if (m.find()) {
            int startYear  = Integer.parseInt(m.group(1));
            int startMonth = Integer.parseInt(m.group(2));
            int startDay   = Integer.parseInt(m.group(3));
            // 종료 연도가 명시되지 않으면 시작 연도 사용
            int endYear    = m.group(4) != null ? Integer.parseInt(m.group(4)) : startYear;
            int endMonth   = Integer.parseInt(m.group(5));
            int endDay     = Integer.parseInt(m.group(6));

            try {
                result[0] = LocalDate.of(startYear, startMonth, startDay);
                result[1] = LocalDate.of(endYear, endMonth, endDay);
            } catch (Exception ignored) {}
        } else {
            // 단일 날짜 시도
            Pattern single = Pattern.compile("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})");
            Matcher ms = single.matcher(raw);
            if (ms.find()) {
                try {
                    LocalDate d = LocalDate.parse(
                            String.format("%s.%02d.%02d", ms.group(1),
                                    Integer.parseInt(ms.group(2)),
                                    Integer.parseInt(ms.group(3))),
                            DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                    result[0] = result[1] = d;
                } catch (DateTimeParseException ignored) {}
            }
        }
        return result;
    }
}
