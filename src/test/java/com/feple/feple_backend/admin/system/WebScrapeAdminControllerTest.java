package com.feple.feple_backend.admin.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.scraper.ScrapedFestivalDto;
import com.feple.feple_backend.admin.scraper.ScraperApplyRequestDto;
import com.feple.feple_backend.admin.scraper.WebScraperService;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WebScrapeAdminControllerTest {

    @Mock WebScraperService webScraperService;
    @Mock FestivalAdminService festivalAdminService;
    @Mock AdminLogService adminLogService;

    @InjectMocks WebScrapeAdminController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── POST /admin/crawl/scrape ──────────────────────────────────────────────

    @Test
    void 스크래핑_URL_없으면_400_반환() throws Exception {
        mockMvc.perform(post("/admin/crawl/scrape")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 스크래핑_성공_결과_반환() throws Exception {
        ScrapedFestivalDto result = mock(ScrapedFestivalDto.class);
        given(webScraperService.scrape(anyString(), anyString())).willReturn(result);

        mockMvc.perform(post("/admin/crawl/scrape")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 스크래핑_IllegalArgument_400_반환() throws Exception {
        given(webScraperService.scrape(anyString(), anyString()))
                .willThrow(new IllegalArgumentException("허용되지 않는 URL"));

        mockMvc.perform(post("/admin/crawl/scrape")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://blocked.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 스크래핑_RuntimeException_500_반환() throws Exception {
        given(webScraperService.scrape(anyString(), anyString()))
                .willThrow(new RuntimeException("연결 실패"));

        mockMvc.perform(post("/admin/crawl/scrape")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isInternalServerError());
    }

    // ── POST /admin/crawl/scrape/apply ────────────────────────────────────────

    @Test
    void 스크래핑_결과_적용_제목없으면_400_반환() throws Exception {
        ScraperApplyRequestDto req = new ScraperApplyRequestDto(
                null, null, null, null, "2026-06-01", "2026-06-03", null, null);

        mockMvc.perform(post("/admin/crawl/scrape/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 스크래핑_결과_적용_성공_festivalId_반환() throws Exception {
        given(festivalAdminService.createFestival(any())).willReturn(1L);

        ScraperApplyRequestDto req = new ScraperApplyRequestDto(
                "테스트 페스티벌", null, null, null,
                "2026-06-01", "2026-06-03", null, null);

        mockMvc.perform(post("/admin/crawl/scrape/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.festivalId").value(1));
    }
}
