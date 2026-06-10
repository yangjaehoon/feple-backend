package com.feple.feple_backend.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiOcrClient {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    private static final String PROMPT = """
            이 이미지는 음악 페스티벌 타임테이블 포스터입니다.
            이미지에서 모든 공연 일정을 추출하여 JSON 배열 형식으로만 반환하세요.
            각 항목 형식: {"artist": "아티스트명", "stage": "스테이지명", "date": "날짜(yyyy-MM-dd) 또는 null", "startTime": "HH:mm", "endTime": "HH:mm", "confidence": 확신도(0~100)}
            규칙:
            - 인식이 불확실한 항목은 confidence를 낮게 설정하세요 (0~100).
            - 스테이지나 시간을 인식할 수 없으면 null로 설정하세요.
            - date: 이미지에 날짜 정보가 명시된 경우(예: "8월 1일", "Day 1 - 2025.08.01", "Aug 1") 반드시 yyyy-MM-dd 형식으로 변환하세요. 날짜가 없거나 연도를 알 수 없으면 null로 설정하세요.
            - 시간은 반드시 24시간 HH:mm 형식으로 변환하세요 (예: 오후 8시→20:00, PM 8:00→20:00).
            - JSON 배열만 반환하고 설명 텍스트나 마크다운 코드블록을 절대 포함하지 마세요.
            """;

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<OcrResultDto> parseTimeTable(MultipartFile image) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(image.getBytes());
        String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", PROMPT),
                                Map.of("inlineData", Map.of(
                                        "mimeType", mimeType,
                                        "data", base64
                                ))
                        )
                )),
                "generationConfig", Map.of("maxOutputTokens", 8192)
        );

        Map response = webClient.post()
                .uri(GEMINI_BASE_URL + "?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(90));

        String content = extractContent(response);
        log.debug("Gemini OCR raw response: {}", content);
        return parseJsonArray(content);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String extractContent(Map response) {
        try {
            List<Map> candidates = (List<Map>) response.get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            log.warn("Gemini OCR 응답 파싱 실패: {}", e.getMessage());
            return "";
        }
    }

    private List<OcrResultDto> parseJsonArray(String content) {
        String json = extractJsonArray(content.trim());
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            // 응답이 maxOutputTokens에 걸려 중간에 잘린 경우 마지막 완성된 객체까지만 복구
            log.warn("OCR JSON parse failed (likely truncated), attempting partial recovery. error: {}", e.getMessage());
            try {
                String partial = recoverPartialArray(json);
                if (partial != null) {
                    List<OcrResultDto> recovered = objectMapper.readValue(partial, new TypeReference<>() {});
                    log.info("OCR partial recovery succeeded: {} entries", recovered.size());
                    return recovered;
                }
            } catch (Exception ex) {
                log.warn("OCR partial recovery also failed", ex);
            }
            return List.of();
        }
    }

    private static String extractJsonArray(String content) {
        Matcher m = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.MULTILINE).matcher(content);
        if (m.find()) return m.group(1).trim();
        int start = content.indexOf('[');
        if (start == -1) return content;
        int end = content.lastIndexOf(']');
        if (end > start) return content.substring(start, end + 1);
        // 닫는 ] 없음 — 응답이 잘린 경우, [ 이후 전체를 반환해 복구 시도
        return content.substring(start);
    }

    private static String recoverPartialArray(String json) {
        // 마지막 완성된 } 위치까지 자르고 배열 닫기
        int lastClose = json.lastIndexOf('}');
        if (lastClose == -1) return null;
        return json.substring(0, lastClose + 1) + "]";
    }
}
