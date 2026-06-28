package com.feple.feple_backend.admin.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.admin.ocr.ArtistLineupOcrResult;
import com.feple.feple_backend.admin.ocr.LineupApplyOcrRequestDto;
import com.feple.feple_backend.admin.ocr.LineupApplyResult;
import com.feple.feple_backend.admin.ocr.OcrApplyRequestDto;
import com.feple.feple_backend.admin.ocr.OcrApplyResultDto;
import com.feple.feple_backend.admin.ocr.OcrResultDto;
import com.feple.feple_backend.admin.ocr.OcrService;
import com.feple.feple_backend.admin.scraper.ScrapedFestivalDto;
import com.feple.feple_backend.admin.scraper.ScraperApplyRequestDto;
import com.feple.feple_backend.admin.scraper.WebScraperService;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.stage.service.StageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CrawlAdminControllerTest {

    @Mock OcrService ocrService;
    @Mock WebScraperService webScraperService;
    @Mock FestivalService festivalService;
    @Mock StageService stageService;
    @Mock ArtistFestivalService artistFestivalService;

    @InjectMocks CrawlAdminController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

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
        given(festivalService.createFestival(any())).willReturn(1L);

        ScraperApplyRequestDto req = new ScraperApplyRequestDto(
                "테스트 페스티벌", null, null, null,
                "2026-06-01", "2026-06-03", null, null);

        mockMvc.perform(post("/admin/crawl/scrape/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.festivalId").value(1));
    }

    // ── POST /admin/crawl/ocr ─────────────────────────────────────────────────

    @Test
    void OCR_파싱_빈_이미지_400_반환() throws Exception {
        MockMultipartFile emptyImage = new MockMultipartFile("image", new byte[0]);

        mockMvc.perform(multipart("/admin/crawl/ocr").file(emptyImage))
                .andExpect(status().isBadRequest());
    }

    @Test
    void OCR_파싱_API키_미설정_503_반환() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg",
                MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});
        given(ocrService.isConfigured()).willReturn(false);

        mockMvc.perform(multipart("/admin/crawl/ocr").file(image))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void OCR_파싱_성공_결과_반환() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg",
                MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});
        given(ocrService.isConfigured()).willReturn(true);
        given(ocrService.parseTimeTable(any())).willReturn(List.of());

        mockMvc.perform(multipart("/admin/crawl/ocr").file(image))
                .andExpect(status().isOk());
    }

    // ── POST /admin/crawl/ocr/apply ───────────────────────────────────────────

    @Test
    void OCR_적용_festivalId없으면_400_반환() throws Exception {
        OcrApplyRequestDto req = new OcrApplyRequestDto(null, List.of());

        mockMvc.perform(post("/admin/crawl/ocr/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void OCR_적용_성공_결과_반환() throws Exception {
        OcrApplyResultDto result = new OcrApplyResultDto(2, 0, List.of());
        given(ocrService.applyEntries(any())).willReturn(result);

        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(mock(OcrResultDto.class)));

        mockMvc.perform(post("/admin/crawl/ocr/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.savedCount").value(2));
    }

    // ── POST /admin/crawl/ocr/lineup ──────────────────────────────────────────

    @Test
    void 라인업_OCR_파싱_API키_미설정_503_반환() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "lineup.jpg",
                MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});
        given(ocrService.isConfigured()).willReturn(false);

        mockMvc.perform(multipart("/admin/crawl/ocr/lineup").file(image))
                .andExpect(status().isServiceUnavailable());
    }

    // ── POST /admin/crawl/ocr/lineup/apply ────────────────────────────────────

    @Test
    void 라인업_OCR_적용_성공_결과_반환() throws Exception {
        given(ocrService.applyArtistLineup(anyLong(), anyList()))
                .willReturn(new LineupApplyResult(2, 2, 0));

        LineupApplyOcrRequestDto req = new LineupApplyOcrRequestDto(1L, List.of(10L, 11L));

        mockMvc.perform(post("/admin/crawl/ocr/lineup/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").value(2));
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
        given(festivalService.getAllFestivals(null, null, null, true, null))
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
