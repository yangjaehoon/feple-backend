package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.ocr.OcrService;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalFilterCriteria;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.stage.service.StageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CrawlAdminControllerTest {

    @Mock OcrService ocrService;
    @Mock FestivalService festivalService;
    @Mock StageService stageService;
    @Mock ArtistFestivalService artistFestivalService;

    @InjectMocks CrawlAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/crawl ──────────────────────────────────────────────────────

    @Test
    void 크롤_대시보드_뷰_반환() throws Exception {
        mockMvc.perform(get("/admin/crawl"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/system/crawl"));
    }

    // ── GET /admin/crawl/quota ────────────────────────────────────────────────

    @Test
    void Gemini_사용량_조회() throws Exception {
        given(ocrService.getTodayUsage()).willReturn(5);
        given(ocrService.getDailyLimit()).willReturn(50);

        mockMvc.perform(get("/admin/crawl/quota"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.used").value(5))
                .andExpect(jsonPath("$.limit").value(50))
                .andExpect(jsonPath("$.remaining").value(45));
    }

    // ── GET /admin/crawl/festivals ────────────────────────────────────────────

    @Test
    void 페스티벌_목록_JSON_반환() throws Exception {
        FestivalResponseDto festival = mock(FestivalResponseDto.class);
        given(festival.getId()).willReturn(1L);
        given(festival.getTitle()).willReturn("테스트 페스티벌");
        given(festivalService.getAllFestivals(FestivalFilterCriteria.forAdmin()))
                .willReturn(List.of(festival));

        mockMvc.perform(get("/admin/crawl/festivals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ── GET /admin/crawl/festivals/{festivalId}/stages ────────────────────────

    @Test
    void 스테이지_목록_JSON_반환() throws Exception {
        given(stageService.getStages(1L)).willReturn(List.of());

        mockMvc.perform(get("/admin/crawl/festivals/1/stages"))
                .andExpect(status().isOk());
    }

    // ── GET /admin/crawl/festivals/{festivalId}/artists ───────────────────────

    @Test
    void 아티스트_목록_JSON_반환() throws Exception {
        given(artistFestivalService.getArtistFestivalsWithEnName(1L)).willReturn(List.of());

        mockMvc.perform(get("/admin/crawl/festivals/1/artists"))
                .andExpect(status().isOk());
    }
}
