package com.feple.feple_backend.admin.scraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.Test;

class FestivalPageScraperTest {

    private final FestivalPageScraper scraper = new FestivalPageScraper(mock(CloseableHttpClient.class));

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
}
