package com.feple.feple_backend.admin.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@ExtendWith(MockitoExtension.class)
class GeminiOcrClientTest {

    @Mock GeminiUsageTracker usageTracker;
    @Mock GeminiApiClient geminiApiClient;

    GeminiOcrClient client;

    private static final Map<String, Object> DUMMY_RESPONSE = Map.of("dummy", "response");

    @BeforeEach
    void setUp() {
        client = new GeminiOcrClient(new ObjectMapper(), usageTracker, geminiApiClient,
                new GeminiProperties("", 500, 16384, 90, 60, 60, 512));
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "poster.jpg", "image/jpeg", new byte[]{1, 2, 3});
    }

    // ── 재시도 / 예비 모델 전환 ───────────────────────────────────────────────

    private static final String PRIMARY = GeminiApiClient.GEMINI_GENERATE_CONTENT_URL;
    private static final String FALLBACK = GeminiApiClient.GEMINI_FALLBACK_GENERATE_CONTENT_URL;

    private static WebClientResponseException googleError(HttpStatus status) {
        return WebClientResponseException.create(status.value(), status.getReasonPhrase(), HttpHeaders.EMPTY, new byte[0], null);
    }

    private static IllegalStateException blockTimeout() {
        return new IllegalStateException("Timeout on blocking read", new TimeoutException());
    }

    private List<String> calledUrls(int callCount) {
        ArgumentCaptor<GeminiApiRequest> captor = ArgumentCaptor.forClass(GeminiApiRequest.class);
        then(geminiApiClient).should(times(callCount)).call(captor.capture());
        return captor.getAllValues().stream().map(GeminiApiRequest::url).toList();
    }

    @Test
    void parseLineup_503이면_기본_모델로_한_번_재시도한다() throws Exception {
        willThrow(googleError(HttpStatus.SERVICE_UNAVAILABLE)).willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("[]");

        client.parseLineup(image(), null);

        assertThat(calledUrls(2)).containsExactly(PRIMARY, PRIMARY);
    }

    @Test
    void parseLineup_503이_반복되면_예비_모델로_전환한다() throws Exception {
        WebClientResponseException overloaded = googleError(HttpStatus.SERVICE_UNAVAILABLE);
        willThrow(overloaded).willThrow(overloaded).willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("[]");

        client.parseLineup(image(), null);

        assertThat(calledUrls(3)).containsExactly(PRIMARY, PRIMARY, FALLBACK);
    }

    @Test
    void parseLineup_타임아웃이면_재시도_없이_예비_모델로_전환하고_호출마다_사용량을_센다() throws Exception {
        willThrow(blockTimeout()).willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("[]");

        client.parseLineup(image(), null);

        assertThat(calledUrls(2)).containsExactly(PRIMARY, FALLBACK);
        then(usageTracker).should(times(2)).increment();
    }

    @Test
    void parseTimetable_429면_재시도_없이_예비_모델로_전환한다() throws Exception {
        willThrow(googleError(HttpStatus.TOO_MANY_REQUESTS)).willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("[]");

        client.parseTimetable(image(), null);

        assertThat(calledUrls(2)).containsExactly(PRIMARY, FALLBACK);
    }

    @Test
    void parseLineup_403이면_재시도와_예비_모델_없이_실패한다() {
        willThrow(googleError(HttpStatus.FORBIDDEN)).given(geminiApiClient).call(any());

        assertThatThrownBy(() -> client.parseLineup(image(), null))
                .isInstanceOf(WebClientResponseException.Forbidden.class);

        assertThat(calledUrls(1)).containsExactly(PRIMARY);
    }

    @Test
    void parseTimetable_정상_JSON_배열을_파싱한다() throws Exception {
        willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn(
                "[{\"artist\":\"아이유\",\"stage\":\"Main\",\"date\":\"2026-08-01\",\"startTime\":\"18:00\",\"endTime\":\"19:00\",\"confidence\":95,\"type\":\"PERFORMANCE\"}]");
        given(geminiApiClient.isTruncated(DUMMY_RESPONSE)).willReturn(false);

        OcrParseResult<TimetableOcrResultDto> result = client.parseTimetable(image(), null);

        assertThat(result.truncated()).isFalse();
        assertThat(result.entries()).hasSize(1);
        assertThat(result.entries().get(0).artist()).isEqualTo("아이유");
        assertThat(result.entries().get(0).isAnnouncement()).isFalse();
    }

    @Test
    void parseTimetable_마크다운_코드블록으로_감싸진_응답도_파싱한다() throws Exception {
        willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn(
                "```json\n[{\"artist\":\"입장 게이트 오픈\",\"stage\":null,\"date\":null,\"startTime\":\"10:00\",\"endTime\":null,\"confidence\":80,\"type\":\"OPS\"}]\n```");
        given(geminiApiClient.isTruncated(DUMMY_RESPONSE)).willReturn(false);

        OcrParseResult<TimetableOcrResultDto> result = client.parseTimetable(image(), null);

        assertThat(result.entries()).hasSize(1);
        assertThat(result.entries().get(0).isAnnouncement()).isTrue();
    }

    @Test
    void parseTimetable_잘린_응답은_마지막_완성된_항목까지_복구한다() throws Exception {
        willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        // 두 번째 객체가 중간에 잘려 닫는 ]가 없는 상황
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn(
                "[{\"artist\":\"아이유\",\"stage\":\"Main\",\"date\":\"2026-08-01\",\"startTime\":\"18:00\",\"endTime\":\"19:00\",\"confidence\":95,\"type\":\"PERFORMANCE\"},{\"artist\":\"잘린항목");
        given(geminiApiClient.isTruncated(DUMMY_RESPONSE)).willReturn(true);

        OcrParseResult<TimetableOcrResultDto> result = client.parseTimetable(image(), null);

        assertThat(result.truncated()).isTrue();
        assertThat(result.entries()).hasSize(1);
        assertThat(result.entries().get(0).artist()).isEqualTo("아이유");
    }

    @Test
    void parseTimetable_완전히_파싱불가능한_응답은_빈_목록을_반환한다() throws Exception {
        willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("이건 JSON이 아닙니다");
        given(geminiApiClient.isTruncated(DUMMY_RESPONSE)).willReturn(false);

        OcrParseResult<TimetableOcrResultDto> result = client.parseTimetable(image(), null);

        assertThat(result.entries()).isEmpty();
    }

    @Test
    void parseLineup_정상_JSON_배열을_파싱한다() throws Exception {
        willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn(
                "[{\"name\":\"아이유\",\"confidence\":90},{\"name\":\"NewJeans\",\"confidence\":85}]");
        given(geminiApiClient.isTruncated(DUMMY_RESPONSE)).willReturn(false);

        OcrParseResult<LineupRawResult> result = client.parseLineup(image(), null);

        assertThat(result.entries()).extracting(LineupRawResult::name)
                .containsExactly("아이유", "NewJeans");
    }

    @Test
    void parseLineup_연도가_주어지면_연도가_반영된_프롬프트로_요청한다() throws Exception {
        willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("[]");
        given(geminiApiClient.isTruncated(DUMMY_RESPONSE)).willReturn(false);

        client.parseLineup(image(), 2026);

        org.mockito.ArgumentCaptor<GeminiApiRequest> captor = org.mockito.ArgumentCaptor.forClass(GeminiApiRequest.class);
        org.mockito.Mockito.verify(geminiApiClient).call(captor.capture());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) captor.getValue().body().get("contents");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
        String promptText = (String) parts.get(0).get("text");
        assertThat(promptText).contains("2026년으로 간주");
    }

    @Test
    void 이미지_처리시마다_사용량을_증가시킨다() throws Exception {
        willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("[]");
        given(geminiApiClient.isTruncated(DUMMY_RESPONSE)).willReturn(false);

        client.parseTimetable(image(), null);

        org.mockito.Mockito.verify(usageTracker).increment();
    }

    @Test
    void 연도가_주어지면_연도가_반영된_프롬프트로_요청한다() throws Exception {
        willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(any());
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("[]");
        given(geminiApiClient.isTruncated(DUMMY_RESPONSE)).willReturn(false);

        client.parseTimetable(image(), 2026);

        org.mockito.ArgumentCaptor<GeminiApiRequest> captor = org.mockito.ArgumentCaptor.forClass(GeminiApiRequest.class);
        org.mockito.Mockito.verify(geminiApiClient).call(captor.capture());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) captor.getValue().body().get("contents");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parts = (List<Map<String, Object>>) contents.get(0).get("parts");
        String promptText = (String) parts.get(0).get("text");
        assertThat(promptText).contains("2026년으로 간주");
    }
}
