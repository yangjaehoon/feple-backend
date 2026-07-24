package com.feple.feple_backend.admin.ocr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GeminiApiClientTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS) WebClient webClient;

    @InjectMocks GeminiApiClient geminiApiClient;

    // ── getNestedValue ────────────────────────────────────────────────────────

    @Test
    void getNestedValue_중첩된_맵과_리스트_경로_탐색() {
        Map<String, Object> response = Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of("parts", List.of(Map.of("text", "hello")))
                ))
        );

        Object result = geminiApiClient.getNestedValue(response, "candidates", 0, "content", "parts", 0, "text");

        assertThat(result).isEqualTo("hello");
    }

    @Test
    void getNestedValue_경로_중간에_null이면_null_반환() {
        Map<String, Object> response = Map.of("candidates", List.of());

        Object result = geminiApiClient.getNestedValue(response, "candidates", 0, "content");

        assertThat(result).isNull();
    }

    @Test
    void getNestedValue_인덱스가_리스트_범위밖이면_null_반환() {
        Map<String, Object> response = Map.of("candidates", List.of(Map.of("a", "b")));

        Object result = geminiApiClient.getNestedValue(response, "candidates", 5);

        assertThat(result).isNull();
    }

    // ── extractText ───────────────────────────────────────────────────────────

    @Test
    void extractText_여러_part의_텍스트를_모두_이어붙임() {
        Map<String, Object> response = Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of("parts", List.of(
                                Map.of("text", "안녕"),
                                Map.of("text", "하세요")
                        ))
                ))
        );

        String result = geminiApiClient.extractText(response);

        assertThat(result).isEqualTo("안녕하세요");
    }

    @Test
    void extractText_parts가_없으면_빈_문자열() {
        Map<String, Object> response = Map.of("candidates", List.of());

        String result = geminiApiClient.extractText(response);

        assertThat(result).isEmpty();
    }

    @Test
    void extractText_응답이_null이면_빈_문자열_반환() {
        String result = geminiApiClient.extractText(null);

        assertThat(result).isEmpty();
    }

    // ── isTruncated ───────────────────────────────────────────────────────────

    @Test
    void isTruncated_finishReason이_MAX_TOKENS이면_true() {
        Map<String, Object> response = Map.of(
                "candidates", List.of(Map.of("finishReason", "MAX_TOKENS"))
        );

        assertThat(geminiApiClient.isTruncated(response)).isTrue();
    }

    @Test
    void isTruncated_finishReason이_STOP이면_false() {
        Map<String, Object> response = Map.of(
                "candidates", List.of(Map.of("finishReason", "STOP"))
        );

        assertThat(geminiApiClient.isTruncated(response)).isFalse();
    }

    @Test
    void isTruncated_finishReason이_없으면_false() {
        Map<String, Object> response = Map.of("candidates", List.of(Map.of()));

        assertThat(geminiApiClient.isTruncated(response)).isFalse();
    }
}
