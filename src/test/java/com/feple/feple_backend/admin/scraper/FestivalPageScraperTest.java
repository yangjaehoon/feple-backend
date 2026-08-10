package com.feple.feple_backend.admin.scraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;

class FestivalPageScraperTest {

    private final FestivalPageScraper scraper = new FestivalPageScraper(mock(CloseableHttpClient.class));

    private FestivalPageScraper scraperReturningHtml(int statusCode, String html) throws Exception {
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        ClassicHttpResponse response = mock(ClassicHttpResponse.class);
        given(response.getCode()).willReturn(statusCode);
        given(response.getEntity()).willReturn(new StringEntity(html, StandardCharsets.UTF_8));
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(response);
        }).when(httpClient).execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
        return new FestivalPageScraper(httpClient);
    }

    @Test
    void 제목_설명_장소가_모두_비어있으면_SPA로_판단한다() {
        ScrapedFestivalDto result = new ScrapedFestivalDto("", "", "", "", "", "", "url", "interpark", null);

        assertThat(scraper.isSpaOrEmpty(result)).isTrue();
    }

    @Test
    void 제목이_SPA_플랫폼명이면_SPA로_판단한다() {
        ScrapedFestivalDto result = new ScrapedFestivalDto("인터파크티켓", "", "", "", "", "", "url", "interpark", null);

        assertThat(scraper.isSpaOrEmpty(result)).isTrue();
    }

    @Test
    void 제목과_설명이_있으면_SPA가_아니다() {
        ScrapedFestivalDto result = new ScrapedFestivalDto(
                "록 페스티벌", "설명입니다", "", "2026-08-01", "2026-08-03", "", "url", "interpark", null);

        assertThat(scraper.isSpaOrEmpty(result)).isFalse();
    }

    @Test
    void 제목이_비어있어도_장소가_있으면_SPA가_아니다() {
        ScrapedFestivalDto result = new ScrapedFestivalDto("", "", "서울숲", "", "", "", "url", "interpark", null);

        assertThat(scraper.isSpaOrEmpty(result)).isFalse();
    }

    // ── scrape ───────────────────────────────────────────────────────────

    @Test
    void scrape_OG태그로_제목_설명_이미지_추출() throws Exception {
        String html = "<html><head>"
                + "<meta property=\"og:title\" content=\"록 페스티벌 2026\">"
                + "<meta property=\"og:description\" content=\"최고의 록 페스티벌\">"
                + "<meta property=\"og:image\" content=\"https://img.example.com/poster.jpg\">"
                + "</head><body></body></html>";
        FestivalPageScraper s = scraperReturningHtml(200, html);

        ScrapedFestivalDto result = s.scrape("http://8.8.8.8/festival", "unknown");

        assertThat(result.title()).isEqualTo("록 페스티벌 2026");
        assertThat(result.description()).isEqualTo("최고의 록 페스티벌");
        assertThat(result.posterImageUrl()).isEqualTo("https://img.example.com/poster.jpg");
        assertThat(result.sourceUrl()).isEqualTo("http://8.8.8.8/festival");
        assertThat(result.source()).isEqualTo("unknown");
        assertThat(result.warning()).isNull();
    }

    @Test
    void scrape_HTTP_에러상태코드면_예외() {
        assertThatThrownBy(() -> scraperReturningHtml(404, "").scrape("http://8.8.8.8/festival", "unknown"))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("404");
    }

    @Test
    void scrape_OG태그_없으면_사이트별_제목_셀렉터_사용() throws Exception {
        String html = "<html><head></head><body>"
                + "<div class=\"GoodsDetail\"><span class=\"title\">인터파크 전용 제목</span></div>"
                + "</body></html>";
        FestivalPageScraper s = scraperReturningHtml(200, html);

        ScrapedFestivalDto result = s.scrape("http://8.8.8.8/festival", "interpark");

        assertThat(result.title()).isEqualTo("인터파크 전용 제목");
    }

    @Test
    void scrape_셀렉터_매칭_없으면_html_title에서_구분자_이후_제거() throws Exception {
        String html = "<html><head><title>페스티벌 제목 | 티켓사이트</title></head><body></body></html>";
        FestivalPageScraper s = scraperReturningHtml(200, html);

        ScrapedFestivalDto result = s.scrape("http://8.8.8.8/festival", "unknown");

        assertThat(result.title()).isEqualTo("페스티벌 제목");
    }

    @Test
    void scrape_테이블_헤더로_장소와_날짜_추출() throws Exception {
        String html = "<html><body><table>"
                + "<tr><th>장소</th><td>서울숲</td></tr>"
                + "<tr><th>기간</th><td>2026.08.01 ~ 2026.08.03</td></tr>"
                + "</table></body></html>";
        FestivalPageScraper s = scraperReturningHtml(200, html);

        ScrapedFestivalDto result = s.scrape("http://8.8.8.8/festival", "unknown");

        assertThat(result.location()).isEqualTo("서울숲");
        assertThat(result.startDate()).isEqualTo("2026-08-01");
        assertThat(result.endDate()).isEqualTo("2026-08-03");
    }

    @Test
    void scrape_테이블에_날짜없으면_JSONLD에서_날짜_폴백() throws Exception {
        String html = "<html><body>"
                + "<script type=\"application/ld+json\">{\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-02\"}</script>"
                + "</body></html>";
        FestivalPageScraper s = scraperReturningHtml(200, html);

        ScrapedFestivalDto result = s.scrape("http://8.8.8.8/festival", "unknown");

        assertThat(result.startDate()).isEqualTo("2026-09-01");
        assertThat(result.endDate()).isEqualTo("2026-09-02");
    }

    @Test
    void scrape_내부_네트워크_URL이면_SSRF_예외() {
        assertThatThrownBy(() -> scraper.scrape("http://127.0.0.1/festival", "unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("내부 네트워크");
    }

    @Test
    void scrape_응답_크기가_상한을_초과하면_예외() throws Exception {
        // 10MB 상한을 넘기는 응답을 흉내내기 위해 InputStream을 직접 구현(문자열로 만들면 메모리 낭비)
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        ClassicHttpResponse response = mock(ClassicHttpResponse.class);
        given(response.getCode()).willReturn(200);
        org.apache.hc.core5.http.HttpEntity entity = mock(org.apache.hc.core5.http.HttpEntity.class);
        given(entity.getContent()).willReturn(new java.io.InputStream() {
            private long remaining = 11L * 1024 * 1024;

            @Override
            public int read() {
                if (remaining <= 0) return -1;
                remaining--;
                return 'a';
            }
        });
        given(response.getEntity()).willReturn(entity);
        doAnswer(invocation -> {
            HttpClientResponseHandler<?> handler = invocation.getArgument(1);
            return handler.handleResponse(response);
        }).when(httpClient).execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));

        assertThatThrownBy(() -> new FestivalPageScraper(httpClient).scrape("http://8.8.8.8/festival", "unknown"))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("응답 크기");
    }
}
