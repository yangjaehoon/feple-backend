package com.feple.feple_backend.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequest;
import com.feple.feple_backend.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String PROMPT = """
            이 이미지는 음악 페스티벌 타임테이블 포스터입니다.
            이미지에서 모든 공연 일정을 추출하여 JSON 배열 형식으로만 반환하세요.
            각 항목 형식: {"artist": "아티스트명", "stage": "스테이지명", "startTime": "HH:mm", "endTime": "HH:mm", "confidence": 확신도(0~100)}
            규칙:
            - 인식이 불확실한 항목은 confidence를 낮게 설정하세요 (0~100).
            - 스테이지나 시간을 인식할 수 없으면 null로 설정하세요.
            - 시간은 반드시 24시간 HH:mm 형식으로 변환하세요 (예: 오후 8시→20:00, PM 8:00→20:00).
            - JSON 배열만 반환하고 설명 텍스트나 마크다운 코드블록을 절대 포함하지 마세요.
            """;

    @Value("${app.openai.api-key:}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final TimetableService timetableService;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<OcrResultDto> parseTimeTable(MultipartFile image) throws IOException {
        String base64 = Base64.getEncoder().encodeToString(image.getBytes());
        String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

        Map<String, Object> request = Map.of(
                "model", "gpt-4o",
                "max_tokens", 4096,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", PROMPT),
                                Map.of("type", "image_url", "image_url",
                                        Map.of("url", "data:" + mimeType + ";base64," + base64))
                        )
                ))
        );

        Map response = webClient.post()
                .uri(OPENAI_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block(Duration.ofSeconds(90));

        String content = extractContent(response);
        log.debug("GPT OCR raw response: {}", content);
        return parseJsonArray(content);
    }

    public OcrApplyResultDto applyEntries(OcrApplyRequest request) {
        List<Map<String, String>> failures = new ArrayList<>();
        int savedCount = 0;

        for (OcrResultDto entry : request.entries()) {
            try {
                TimetableEntryRequest req = new TimetableEntryRequest();
                req.setStageName(entry.stage() != null ? entry.stage().trim() : "");
                req.setArtistName(entry.artist() != null ? entry.artist().trim() : "");
                req.setFestivalDate(request.festivalDate());
                req.setStartTime(LocalTime.parse(entry.startTime()));
                req.setEndTime(LocalTime.parse(entry.endTime()));
                timetableService.createEntry(request.festivalId(), req);
                savedCount++;
            } catch (Exception e) {
                Map<String, String> failure = new HashMap<>();
                failure.put("artist", entry.artist() != null ? entry.artist() : "—");
                failure.put("stage",  entry.stage()  != null ? entry.stage()  : "—");
                failure.put("reason", e.getMessage());
                failures.add(failure);
            }
        }
        return new OcrApplyResultDto(savedCount, failures.size(), failures);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String extractContent(Map response) {
        List<Map> choices = (List<Map>) response.get("choices");
        Map message = (Map) choices.get(0).get("message");
        return (String) message.get("content");
    }

    private List<OcrResultDto> parseJsonArray(String content) {
        String json = stripMarkdown(content.trim());
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("OCR JSON parse failed. raw: {}", content, e);
            return List.of();
        }
    }

    private static String stripMarkdown(String content) {
        Matcher m = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.MULTILINE).matcher(content);
        if (m.find()) return m.group(1).trim();
        int start = content.indexOf('[');
        int end   = content.lastIndexOf(']');
        if (start != -1 && end > start) return content.substring(start, end + 1);
        return content;
    }
}
