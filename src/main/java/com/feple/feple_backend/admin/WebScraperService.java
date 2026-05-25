package com.feple.feple_backend.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebScraperService {

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private static final String USER_AGENT =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{4})[.\\-/년]\\s*(\\d{1,2})[.\\-/월]\\s*(\\d{1,2})"
    );

    // SPA 껍데기로 판단하는 사이트별 기본 title
    private static final List<String> SPA_FALLBACK_TITLES = List.of(
        "NOL 티켓", "NOL 인터파크", "인터파크티켓", "인터파크 티켓",
        "예스24 티켓", "YES24", "멜론티켓", "MELON TICKET",
        "티켓링크", "하나티켓", "알티켓"
    );

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ScrapedFestivalDto scrape(String url, String source) throws IOException {
        // 1단계: Jsoup으로 정적 HTML 파싱
        ScrapedFestivalDto jsoupResult = scrapeWithJsoup(url, source);

        // 2단계: SPA 감지 → Gemini URL context 폴백
        if (isSpaOrEmpty(jsoupResult) && isGeminiAvailable()) {
            log.info("SPA detected, falling back to Gemini URL context for: {}", url);
            try {
                return scrapeWithGemini(url, source);
            } catch (Exception e) {
                log.warn("Gemini URL context failed for {}: {}", url, e.getMessage());
                // Gemini 실패 시 빈 결과 반환 (Jsoup보다 낫지 않음)
            }
        }

        return jsoupResult;
    }

    // ── Jsoup 파싱 ────────────────────────────────────────────────────────────

    private ScrapedFestivalDto scrapeWithJsoup(String url, String source) throws IOException {
        Document doc = Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .referrer("https://www.google.com")
            .timeout(15_000)
            .get();

        String title       = extractTitle(doc, source);
        String description = extractDescription(doc, source);
        String imageUrl    = extractImageUrl(doc);
        String location    = extractLocation(doc, source);
        String[] dates     = extractDates(doc, source);

        log.debug("Jsoup scraped [{}] {} → title={}", source, url, title);

        return new ScrapedFestivalDto(title, description, location,
            dates[0], dates[1], imageUrl, url, source, null);
    }

    private String extractTitle(Document doc, String source) {
        for (String sel : new String[]{
            "meta[property=og:title]", "meta[name=twitter:title]"
        }) {
            String v = doc.select(sel).attr("content").trim();
            if (!v.isBlank() && !isSpaTitle(v)) return v;
        }

        String specific = switch (source) {
            case "interpark" -> firstNonEmpty(
                doc.select(".GoodsDetail .title").text(),
                doc.select(".box_concert_name span").text(),
                doc.select("h3.tit_goods").text(),
                doc.select(".goods_name").text()
            );
            case "yes24" -> firstNonEmpty(
                doc.select(".goods_name h2").text(),
                doc.select(".tit_goods").text(),
                doc.select("h2.info_title").text()
            );
            case "melon" -> firstNonEmpty(
                doc.select(".subject_wrap .subject").text(),
                doc.select(".info_tit").text(),
                doc.select(".concert_tit").text()
            );
            default -> "";
        };
        if (!specific.isBlank()) return specific;

        String htmlTitle = doc.title().replaceAll("\\s*[|\\-–—].*", "").trim();
        return isSpaTitle(htmlTitle) ? "" : htmlTitle;
    }

    private String extractDescription(Document doc, String source) {
        for (String sel : new String[]{
            "meta[property=og:description]", "meta[name=twitter:description]", "meta[name=description]"
        }) {
            String v = doc.select(sel).attr("content").trim();
            if (!v.isBlank()) return v;
        }
        return switch (source) {
            case "interpark" -> firstNonEmpty(
                doc.select(".goods_detail_info").text(),
                doc.select(".box_con_detail .con").text()
            );
            case "yes24" -> firstNonEmpty(doc.select(".goods_intro").text());
            default -> "";
        };
    }

    private String extractImageUrl(Document doc) {
        for (String sel : new String[]{
            "meta[property=og:image]", "meta[name=twitter:image]", "meta[name=twitter:image:src]"
        }) {
            String v = doc.select(sel).attr("content").trim();
            if (!v.isBlank()) return v;
        }
        return "";
    }

    private String extractLocation(Document doc, String source) {
        String[] headers = switch (source) {
            case "interpark" -> new String[]{"장소", "공연장소", "행사장소"};
            case "yes24"     -> new String[]{"공연장소", "장소", "공연장"};
            case "melon"     -> new String[]{"장소", "공연장소"};
            default          -> new String[]{"장소", "공연장소", "공연장", "행사장소"};
        };
        return extractTableCell(doc, headers);
    }

    private String[] extractDates(Document doc, String source) {
        String[] headers = switch (source) {
            case "interpark" -> new String[]{"기간", "공연기간", "행사기간"};
            case "yes24"     -> new String[]{"공연기간", "기간", "행사기간"};
            case "melon"     -> new String[]{"기간", "공연기간"};
            default          -> new String[]{"기간", "공연기간", "행사기간"};
        };

        String raw = extractTableCell(doc, headers);
        if (!raw.isBlank()) return parseDateRange(raw);

        for (Element script : doc.select("script[type=application/ld+json]")) {
            String json = script.html();
            String start = extractJsonValue(json, "startDate");
            String end   = extractJsonValue(json, "endDate");
            if (!start.isBlank()) {
                return new String[]{normalizeDate(start), normalizeDate(end.isBlank() ? start : end)};
            }
        }
        return new String[]{"", ""};
    }

    private String extractTableCell(Document doc, String[] headers) {
        for (String header : headers) {
            for (Element th : doc.select("th, dt")) {
                String text = th.text().trim();
                if (text.equals(header) || text.startsWith(header)) {
                    Element next = th.nextElementSibling();
                    if (next != null && !next.text().isBlank()) return next.text().trim();
                }
            }
        }
        return "";
    }

    // ── Gemini URL context 폴백 ───────────────────────────────────────────────

    private ScrapedFestivalDto scrapeWithGemini(String url, String source) {
        String prompt = """
            아래 URL의 공연/페스티벌 페이지에서 정보를 추출하세요.
            다음 형식의 JSON 객체만 반환하세요 (마크다운·설명 텍스트 없이):
            {"title":"공연명","description":"설명(200자 이내)","location":"공연장소","startDate":"YYYY-MM-DD","endDate":"YYYY-MM-DD","posterImageUrl":"포스터 이미지 URL"}
            값을 찾을 수 없는 필드는 빈 문자열("")로 설정하세요.
            URL: """ + url;

        Map<String, Object> request = Map.of(
            "tools", List.of(Map.of("url_context", Map.of())),
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            )),
            "generationConfig", Map.of("maxOutputTokens", 512, "temperature", 0)
        );

        @SuppressWarnings("rawtypes")
        Map response = webClient.post()
            .uri(GEMINI_URL + "?key=" + geminiApiKey)
            .header("Content-Type", "application/json")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .block(Duration.ofSeconds(60));

        // URL 접근 차단 여부 먼저 확인
        if (isUrlBlocked(response)) {
            log.warn("Gemini URL context blocked for: {}", url);
            return new ScrapedFestivalDto("", "", "", "", "", "", url, source,
                "이 사이트는 외부 접근이 차단되어 있습니다. 페이지 스크린샷을 'OCR 파싱' 탭에 업로드하면 정보를 자동 추출할 수 있습니다.");
        }

        String raw = extractGeminiText(response);
        log.debug("Gemini URL context raw: {}", raw);

        return parseGeminiFestivalJson(raw, url, source);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean isUrlBlocked(Map response) {
        try {
            var candidates = (java.util.List<Map>) response.get("candidates");
            Map candidate = candidates.get(0);
            Map urlMeta = (Map) candidate.get("urlContextMetadata");
            if (urlMeta == null) return false;
            var urlMetadata = (java.util.List<Map>) urlMeta.get("urlMetadata");
            if (urlMetadata == null || urlMetadata.isEmpty()) return false;
            String st = (String) urlMetadata.get(0).get("urlRetrievalStatus");
            return "URL_RETRIEVAL_STATUS_ERROR".equals(st);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String extractGeminiText(Map response) {
        try {
            var candidates = (java.util.List<Map>) response.get("candidates");
            var content    = (Map) candidates.get(0).get("content");
            if (content == null) return "";
            var parts = (java.util.List<Map>) content.get("parts");
            if (parts == null) return "";
            // 모든 parts의 text를 합쳐서 반환
            StringBuilder sb = new StringBuilder();
            for (Map part : parts) {
                Object text = part.get("text");
                if (text instanceof String s && !s.isBlank()) sb.append(s);
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Failed to extract Gemini response text", e);
            return "";
        }
    }

    private ScrapedFestivalDto parseGeminiFestivalJson(String raw, String url, String source) {
        if (raw == null || raw.isBlank()) {
            return new ScrapedFestivalDto("", "", "", "", "", "", url, source,
                "자동 추출에 실패했습니다. 직접 입력해주세요.");
        }

        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start == -1 || end <= start) {
            log.warn("Gemini did not return JSON. raw={}", raw);
            return new ScrapedFestivalDto("", "", "", "", "", "", url, source,
                "자동 추출에 실패했습니다. 직접 입력해주세요.");
        }
        String json = raw.substring(start, end + 1);

        try {
            JsonNode node = objectMapper.readTree(json);
            return new ScrapedFestivalDto(
                textOf(node, "title"),
                textOf(node, "description"),
                textOf(node, "location"),
                textOf(node, "startDate"),
                textOf(node, "endDate"),
                textOf(node, "posterImageUrl"),
                url,
                source,
                null
            );
        } catch (Exception e) {
            log.warn("Failed to parse Gemini festival JSON: {}", json, e);
            return new ScrapedFestivalDto("", "", "", "", "", "", url, source,
                "자동 추출에 실패했습니다. 직접 입력해주세요.");
        }
    }

    private String textOf(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? "" : n.asText("").trim();
    }

    // ── 공통 유틸 ─────────────────────────────────────────────────────────────

    private boolean isSpaOrEmpty(ScrapedFestivalDto r) {
        return (r.title().isBlank() || isSpaTitle(r.title()))
            && r.description().isBlank()
            && r.location().isBlank();
    }

    private boolean isSpaTitle(String title) {
        if (title == null || title.isBlank()) return true;
        String lower = title.toLowerCase();
        return SPA_FALLBACK_TITLES.stream().anyMatch(t -> lower.contains(t.toLowerCase()));
    }

    private boolean isGeminiAvailable() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    private String[] parseDateRange(String raw) {
        Matcher m = DATE_PATTERN.matcher(raw);
        String start = "", end = "";
        if (m.find()) {
            start = formatDate(m.group(1), m.group(2), m.group(3));
            if (m.find()) {
                end = formatDate(m.group(1), m.group(2), m.group(3));
            } else {
                end = start;
            }
        }
        return new String[]{start, end};
    }

    private String formatDate(String year, String month, String day) {
        try {
            return LocalDate.of(
                Integer.parseInt(year),
                Integer.parseInt(month.trim()),
                Integer.parseInt(day.trim())
            ).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizeDate(String raw) {
        if (raw == null || raw.isBlank()) return "";
        if (raw.matches("\\d{4}-\\d{2}-\\d{2}.*")) return raw.substring(0, 10);
        return parseDateRange(raw)[0];
    }

    private String extractJsonValue(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private String firstNonEmpty(String... candidates) {
        for (String s : candidates) {
            if (s != null && !s.isBlank()) return s.trim();
        }
        return "";
    }
}
