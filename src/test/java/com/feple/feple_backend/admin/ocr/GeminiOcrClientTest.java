package com.feple.feple_backend.admin.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class GeminiOcrClientTest {

    @Mock GeminiUsageTracker usageTracker;
    @Mock GeminiApiClient geminiApiClient;

    GeminiOcrClient client;

    private static final Map<String, Object> DUMMY_RESPONSE = Map.of("dummy", "response");

    @BeforeEach
    void setUp() {
        client = new GeminiOcrClient(new ObjectMapper(), usageTracker, geminiApiClient,
                new GeminiProperties("", 500, 16384, 90, 60, 512));
    }

    private MockMultipartFile image() {
        return new MockMultipartFile("image", "poster.jpg", "image/jpeg", new byte[]{1, 2, 3});
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
