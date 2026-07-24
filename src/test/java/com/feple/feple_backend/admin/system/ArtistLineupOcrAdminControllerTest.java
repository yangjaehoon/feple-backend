package com.feple.feple_backend.admin.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.ocr.LineupApplyResult;
import com.feple.feple_backend.admin.ocr.LineupOcrApplyRequestDto;
import com.feple.feple_backend.admin.ocr.ArtistLineupOcrService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArtistLineupOcrAdminControllerTest {

    @Mock ArtistLineupOcrService ocrService;
    @Mock AdminLogService adminLogService;

    @InjectMocks ArtistLineupOcrAdminController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
        given(ocrService.applyArtistLineup(any(LineupOcrApplyRequestDto.class)))
                .willReturn(new LineupApplyResult(2, 2, 0));

        LineupOcrApplyRequestDto req = new LineupOcrApplyRequestDto(1L, List.of(10L, 11L), List.of());

        mockMvc.perform(post("/admin/crawl/ocr/lineup/apply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requested").value(2));
    }
}
