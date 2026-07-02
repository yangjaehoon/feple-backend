package com.feple.feple_backend.admin.ocr;

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
    private static final String LINEUP_PROMPT = """
            이 이미지는 음악 페스티벌 라인업 포스터입니다.
            이미지에 나타난 모든 출연 아티스트 이름을 추출하여 JSON 배열 형식으로만 반환하세요.
            각 항목 형식: {"name": "아티스트명", "confidence": 확신도(0~100)}
            규칙:
            - 페스티벌 이름, 날짜, 장소, 스폰서, 주최사 등 아티스트가 아닌 텍스트는 제외하세요.
            - 동일 아티스트가 여러 번 표시되면 한 번만 포함하세요.
            - 인식이 불확실한 이름은 confidence를 낮게 설정하세요 (0~100).
            - JSON 배열만 반환하고 설명 텍스트나 마크다운 코드블록을 절대 포함하지 마세요.
            """;
    private static final String PROMPT = """
            이 이미지는 음악 페스티벌 타임테이블 포스터입니다.
            이미지에서 모든 일정(공연 및 운영 항목)을 추출하여 JSON 배열 형식으로만 반환하세요.
            각 항목 형식: {"artist": "아티스트명 또는 운영 항목명", "stage": "스테이지명 또는 null", "date": "날짜(yyyy-MM-dd) 또는 null", "startTime": "HH:mm", "endTime": "HH:mm", "confidence": 확신도(0~100), "type": "PERFORMANCE 또는 OPS"}
            규칙:
            - type: 아티스트 공연이면 "PERFORMANCE", 티켓 부스 오픈/입장 게이트 오픈 등 운영 항목이면 "OPS"로 설정하세요.
            - 운영 항목(type: "OPS")의 artist 필드에는 항목명(예: "티켓 부스 오픈", "입장 게이트 오픈")을 입력하고, stage는 null로 설정하세요.
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
    private final GeminiUsageTracker usageTracker;

    boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    int getTodayUsage() {
        return usageTracker.getTodayCount();
    }

    int getDailyLimit() {
        return usageTracker.getDailyLimit();
    }

    public List<OcrResultDto> parseTimeTable(MultipartFile image, Integer year) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(image.getBytes());
        String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";
        String prompt = year != null ? buildPromptWithYear(year) : PROMPT;
        String content = extractContent(callGeminiApi(buildGeminiRequest(prompt, base64, mimeType)));
        log.debug("Gemini OCR raw response: {}", content);
        return parseJsonArray(content);
    }

    private String buildPromptWithYear(int year) {
        return PROMPT + "\n- 이미지에 연도가 명시되지 않은 날짜(예: \"8월 1일\", \"8/1\", \"Aug 1\", \"Day 1\", \"Day 2\")는 "
                + year + "년으로 간주하여 " + year + "-MM-dd 형식으로 변환하세요.";
    }

    public List<LineupRawResult> parseLineup(MultipartFile image) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(image.getBytes());
        String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";
        String content = extractContent(callGeminiApi(buildGeminiRequest(LINEUP_PROMPT, base64, mimeType)));
        log.debug("Gemini Lineup OCR raw response: {}", content);
        return parseLineupJsonArray(content);
    }

    private Map<String, Object> buildGeminiRequest(String prompt, String base64, String mimeType) {
        return Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("text", prompt),
                                Map.of("inlineData", Map.of(
                                        "mimeType", mimeType,
                                        "data", base64
                                ))
                        )
                )),
                "generationConfig", Map.of("maxOutputTokens", 8192)
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<?, ?> callGeminiApi(Map<String, Object> request) {
        usageTracker.increment();
        return webClient.post()
                .uri(GEMINI_BASE_URL)
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(90));
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

    private List<LineupRawResult> parseLineupJsonArray(String content) {
        String json = extractJsonArray(content.trim());
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Lineup OCR JSON parse failed, attempting partial recovery. error: {}", e.getMessage());
            try {
                String partial = recoverPartialArray(json);
                if (partial != null) {
                    return objectMapper.readValue(partial, new TypeReference<>() {});
                }
            } catch (Exception ex) {
                log.warn("Lineup OCR partial recovery also failed", ex);
            }
            return List.of();
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
        Matcher matcher = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.MULTILINE).matcher(content);
        if (matcher.find()) return matcher.group(1).trim();
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
