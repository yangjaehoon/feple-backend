package com.feple.feple_backend.admin.ocr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.admin.scraper.ScrapedFestivalDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiUrlContextClient {

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper;
    private final GeminiApiClient geminiApiClient;
    private final GeminiUsageTracker usageTracker;

    public boolean isConfigured() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    public ScrapedFestivalDto scrape(String url, String source) {
        Map<String, Object> request = buildUrlContextRequest(url);
        usageTracker.increment();
        Map<?, ?> response = geminiApiClient.call(GEMINI_URL, geminiApiKey, request, Duration.ofSeconds(60));

        if (isUrlBlocked(response)) {
            log.warn("Gemini URL context blocked for: {}", url);
            return new ScrapedFestivalDto("", "", "", "", "", "", url, source,
                "이 사이트는 외부 접근이 차단되어 있습니다. 페이지 스크린샷을 'OCR 파싱' 탭에 업로드하면 정보를 자동 추출할 수 있습니다.");
        }

        String raw = geminiApiClient.extractText(response);
        log.debug("Gemini URL context raw: {}", raw);

        return parseGeminiFestivalJson(raw, url, source);
    }

    private Map<String, Object> buildUrlContextRequest(String url) {
        String prompt = """
            아래 URL의 공연/페스티벌 페이지에서 정보를 추출하세요.
            다음 형식의 JSON 객체만 반환하세요 (마크다운·설명 텍스트 없이):
            {"title":"공연명","description":"설명(200자 이내)","location":"공연장소","startDate":"YYYY-MM-DD","endDate":"YYYY-MM-DD","posterImageUrl":"포스터 이미지 URL"}
            값을 찾을 수 없는 필드는 빈 문자열("")로 설정하세요.
            URL: """ + url;

        return Map.of(
            "tools", List.of(Map.of("url_context", Map.of())),
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            )),
            "generationConfig", Map.of("maxOutputTokens", 512, "temperature", 0)
        );
    }

    private boolean isUrlBlocked(Map<?, ?> response) {
        try {
            Object status = geminiApiClient.getNestedValue(response, "candidates", 0,
                    "urlContextMetadata", "urlMetadata", 0, "urlRetrievalStatus");
            return "URL_RETRIEVAL_STATUS_ERROR".equals(status);
        } catch (Exception e) {
            return false;
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
        JsonNode fieldNode = node.get(field);
        return (fieldNode == null || fieldNode.isNull()) ? "" : fieldNode.asText("").trim();
    }
}
