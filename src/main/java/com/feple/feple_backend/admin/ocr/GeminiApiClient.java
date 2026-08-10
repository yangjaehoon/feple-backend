package com.feple.feple_backend.admin.ocr;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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

    // 특정 버전을 고정하면 Google이 그 모델을 폐기할 때마다(2026-08-02, gemini-2.5-flash가
    // "no longer available to new users" 404로 OCR 파싱이 전부 실패했음) 코드를 바꿔야 한다 —
    // "latest" 별칭은 Google이 알아서 현재 세대 모델로 갱신해준다.
    static final String GEMINI_GENERATE_CONTENT_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    private final WebClient geminiWebClient;

    @SuppressWarnings("unchecked")
    Map<?, ?> call(GeminiApiRequest request) {
        return geminiWebClient.post()
                .uri(request.url())
                .header("x-goog-api-key", request.apiKey())
                .header("Content-Type", "application/json")
                .bodyValue(request.body())
                .retrieve()
                .bodyToMono(Map.class)
                .block(request.timeout());
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
