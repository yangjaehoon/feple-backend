package com.feple.feple_backend.admin.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class GeminiApiClientTest {

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS) WebClient webClient;

    @InjectMocks GeminiApiClient geminiApiClient;

    // ── call ──────────────────────────────────────────────────────────────────

    @Test
    void call_구글이_오류를_반환하면_응답_본문을_로그에_남기고_예외를_전파() {
        String errorBody = "{\"error\":{\"status\":\"UNAVAILABLE\",\"message\":\"The model is overloaded.\"}}";
        WebClient failingClient = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(errorBody)
                        .build()))
                .build();
        GeminiApiRequest request = new GeminiApiRequest("https://gemini.test", "key", Map.of(), Duration.ofSeconds(5));
        Logger logger = (Logger) LoggerFactory.getLogger(GeminiApiClient.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThatThrownBy(() -> new GeminiApiClient(failingClient).call(request))
                    .isInstanceOf(WebClientResponseException.ServiceUnavailable.class);
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).singleElement()
                .satisfies(event -> assertThat(event.getFormattedMessage()).contains("status=503", errorBody));
    }

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
