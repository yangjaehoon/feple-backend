package com.feple.feple_backend.admin.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.admin.scraper.ScrapedFestivalDto;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GeminiUrlContextClientTest {

    @Mock GeminiApiClient geminiApiClient;
    @Mock GeminiUsageTracker usageTracker;

    GeminiUrlContextClient client;

    private static final Map<String, Object> DUMMY_RESPONSE = Map.of("dummy", "response");
    private static final String URL = "https://example.com/festival";
    private static final String SOURCE = "interpark";

    @BeforeEach
    void setUp() {
        client = new GeminiUrlContextClient(new ObjectMapper(), geminiApiClient, usageTracker);
        willReturn(DUMMY_RESPONSE).given(geminiApiClient).call(anyString(), any(), any(), any());
    }

    private void stubNotBlocked() {
        given(geminiApiClient.getNestedValue(any(), any(), any(), any(), any(), any(), any())).willReturn(null);
    }

    @Test
    void apiKey가_설정되어_있으면_isConfigured는_true() {
        ReflectionTestUtils.setField(client, "geminiApiKey", "test-key");
        assertThat(client.isConfigured()).isTrue();
    }

    @Test
    void apiKey가_빈값이면_isConfigured는_false() {
        ReflectionTestUtils.setField(client, "geminiApiKey", "  ");
        assertThat(client.isConfigured()).isFalse();
    }

    @Test
    void 정상_응답이면_설명_텍스트에서_JSON을_추출해_반환한다() {
        stubNotBlocked();
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn(
                "다음은 결과입니다:\n{\"title\":\"록 페스티벌\",\"description\":\"설명\",\"location\":\"서울숲\",\"startDate\":\"2026-08-01\",\"endDate\":\"2026-08-03\",\"posterImageUrl\":\"img.jpg\"}\n감사합니다.");

        ScrapedFestivalDto result = client.scrape(URL, SOURCE);

        assertThat(result.title()).isEqualTo("록 페스티벌");
        assertThat(result.location()).isEqualTo("서울숲");
        assertThat(result.startDate()).isEqualTo("2026-08-01");
        assertThat(result.warning()).isNull();
        verify(usageTracker).increment();
    }

    @Test
    void URL이_차단되면_경고_메시지와_함께_빈_결과를_반환하고_텍스트_추출은_생략한다() {
        given(geminiApiClient.getNestedValue(any(), any(), any(), any(), any(), any(), any()))
                .willReturn("URL_RETRIEVAL_STATUS_ERROR");

        ScrapedFestivalDto result = client.scrape(URL, SOURCE);

        assertThat(result.title()).isEmpty();
        assertThat(result.warning()).contains("OCR 파싱");
        verify(geminiApiClient, never()).extractText(any());
    }

    @Test
    void 응답이_비어있으면_실패_결과를_반환한다() {
        stubNotBlocked();
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("");

        ScrapedFestivalDto result = client.scrape(URL, SOURCE);

        assertThat(result.title()).isEmpty();
        assertThat(result.warning()).isEqualTo("자동 추출에 실패했습니다. 직접 입력해주세요.");
    }

    @Test
    void JSON이_아닌_응답이면_실패_결과를_반환한다() {
        stubNotBlocked();
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("죄송합니다, 정보를 찾을 수 없습니다.");

        ScrapedFestivalDto result = client.scrape(URL, SOURCE);

        assertThat(result.warning()).isEqualTo("자동 추출에 실패했습니다. 직접 입력해주세요.");
    }

    @Test
    void 필드가_누락된_JSON은_빈문자열로_채운다() {
        stubNotBlocked();
        given(geminiApiClient.extractText(DUMMY_RESPONSE)).willReturn("{\"title\":\"제목만 있음\"}");

        ScrapedFestivalDto result = client.scrape(URL, SOURCE);

        assertThat(result.title()).isEqualTo("제목만 있음");
        assertThat(result.location()).isEmpty();
        assertThat(result.warning()).isNull();
    }
}
