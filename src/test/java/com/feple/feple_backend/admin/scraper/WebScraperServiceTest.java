package com.feple.feple_backend.admin.scraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.admin.ocr.GeminiUrlContextClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebScraperServiceTest {

    @Mock FestivalPageScraper festivalPageScraper;
    @Mock GeminiUrlContextClient geminiUrlContextClient;

    @InjectMocks WebScraperService webScraperService;

    @Test
    void SPA가_아니면_Jsoup_결과를_그대로_반환한다() throws Exception {
        ScrapedFestivalDto jsoupResult = new ScrapedFestivalDto(
                "제목", "설명", "장소", "2026-08-01", "2026-08-03", "img", "url", "interpark", null);
        given(festivalPageScraper.scrape("url", "interpark")).willReturn(jsoupResult);
        given(festivalPageScraper.isSpaOrEmpty(jsoupResult)).willReturn(false);

        ScrapedFestivalDto result = webScraperService.scrape("url", "interpark");

        assertThat(result).isEqualTo(jsoupResult);
        verify(geminiUrlContextClient, never()).scrape(anyString(), anyString());
    }

    @Test
    void SPA이고_Gemini가_설정되어_있으면_Gemini_결과로_대체한다() throws Exception {
        ScrapedFestivalDto jsoupResult = new ScrapedFestivalDto(
                "", "", "", "", "", "", "url", "interpark", null);
        ScrapedFestivalDto geminiResult = new ScrapedFestivalDto(
                "제목", "설명", "장소", "2026-08-01", "2026-08-03", "img", "url", "interpark", null);
        given(festivalPageScraper.scrape("url", "interpark")).willReturn(jsoupResult);
        given(festivalPageScraper.isSpaOrEmpty(jsoupResult)).willReturn(true);
        given(geminiUrlContextClient.isConfigured()).willReturn(true);
        given(geminiUrlContextClient.scrape("url", "interpark")).willReturn(geminiResult);

        ScrapedFestivalDto result = webScraperService.scrape("url", "interpark");

        assertThat(result).isEqualTo(geminiResult);
    }

    @Test
    void SPA이지만_Gemini가_설정되지_않았으면_Jsoup_결과를_그대로_반환한다() throws Exception {
        ScrapedFestivalDto jsoupResult = new ScrapedFestivalDto(
                "", "", "", "", "", "", "url", "interpark", null);
        given(festivalPageScraper.scrape("url", "interpark")).willReturn(jsoupResult);
        given(festivalPageScraper.isSpaOrEmpty(jsoupResult)).willReturn(true);
        given(geminiUrlContextClient.isConfigured()).willReturn(false);

        ScrapedFestivalDto result = webScraperService.scrape("url", "interpark");

        assertThat(result).isEqualTo(jsoupResult);
        verify(geminiUrlContextClient, never()).scrape(anyString(), anyString());
    }

    @Test
    void SPA이고_Gemini_호출이_실패하면_Jsoup_결과로_폴백한다() throws Exception {
        ScrapedFestivalDto jsoupResult = new ScrapedFestivalDto(
                "", "", "", "", "", "", "url", "interpark", null);
        given(festivalPageScraper.scrape("url", "interpark")).willReturn(jsoupResult);
        given(festivalPageScraper.isSpaOrEmpty(jsoupResult)).willReturn(true);
        given(geminiUrlContextClient.isConfigured()).willReturn(true);
        given(geminiUrlContextClient.scrape(any(), any())).willThrow(new RuntimeException("Gemini 오류"));

        ScrapedFestivalDto result = webScraperService.scrape("url", "interpark");

        assertThat(result).isEqualTo(jsoupResult);
    }
}
