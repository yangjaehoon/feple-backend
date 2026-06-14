package com.feple.feple_backend.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    boolean isConfigured() {
        return geminiApiKey != null && !geminiApiKey.isBlank();
    }

    ScrapedFestivalDto scrape(String url, String source) {
        Map<String, Object> request = buildUrlContextRequest(url);
        Map<?, ?> response = callGeminiApi(request);

        if (isUrlBlocked(response)) {
            log.warn("Gemini URL context blocked for: {}", url);
            return new ScrapedFestivalDto("", "", "", "", "", "", url, source,
                "이 사이트는 외부 접근이 차단되어 있습니다. 페이지 스크린샷을 'OCR 파싱' 탭에 업로드하면 정보를 자동 추출할 수 있습니다.");
        }

        String raw = extractGeminiText(response);
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

    @SuppressWarnings("unchecked")
    private Map<?, ?> callGeminiApi(Map<String, Object> request) {
        return webClient.post()
            .uri(GEMINI_URL)
            .header("x-goog-api-key", geminiApiKey)
            .header("Content-Type", "application/json")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .block(Duration.ofSeconds(60));
    }

    private boolean isUrlBlocked(Map<?, ?> response) {
        try {
            Object status = getNestedValue(response, "candidates", 0,
                    "urlContextMetadata", "urlMetadata", 0, "urlRetrievalStatus");
            return "URL_RETRIEVAL_STATUS_ERROR".equals(status);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractGeminiText(Map<?, ?> response) {
        try {
            Object partsObj = getNestedValue(response, "candidates", 0, "content", "parts");
            if (!(partsObj instanceof List<?> parts)) return "";
            StringBuilder sb = new StringBuilder();
            for (Object part : parts) {
                if (part instanceof Map<?, ?> map) {
                    Object text = map.get("text");
                    if (text instanceof String s && !s.isBlank()) sb.append(s);
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.warn("Failed to extract Gemini response text", e);
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Object current, Object... path) {
        for (Object key : path) {
            if (current == null) return null;
            if (key instanceof Integer i && current instanceof List<?> list)
                current = i < list.size() ? list.get(i) : null;
            else if (key instanceof String s && current instanceof Map<?, ?> map)
                current = map.get(s);
            else
                return null;
        }
        return current;
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
}
