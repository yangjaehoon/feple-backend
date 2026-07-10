package com.feple.feple_backend.admin.ocr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Gemini generateContent API 호출 + 응답 파싱 공통 로직.
 * GeminiOcrClient(이미지 OCR)와 GeminiUrlContextClient(URL 컨텍스트)가 같은
 * API 응답 구조(candidates[].content.parts[].text)를 각자 다른 방식으로
 * 파싱하던 것을 하나로 합쳤다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class GeminiApiClient {

    private final WebClient geminiWebClient;

    @SuppressWarnings("unchecked")
    Map<?, ?> call(String url, String apiKey, Map<String, Object> requestBody, Duration timeout) {
        return geminiWebClient.post()
                .uri(url)
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block(timeout);
    }

    Object getNestedValue(Object current, Object... path) {
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

    /** Gemini가 maxOutputTokens 한도에 걸려 응답을 중간에 끊었는지 여부 */
    boolean isTruncated(Map<?, ?> response) {
        Object finishReason = getNestedValue(response, "candidates", 0, "finishReason");
        return "MAX_TOKENS".equals(finishReason);
    }

    String extractText(Map<?, ?> response) {
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
}
