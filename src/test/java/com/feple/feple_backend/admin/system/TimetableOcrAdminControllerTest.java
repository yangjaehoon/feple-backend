package com.feple.feple_backend.admin.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.ocr.OcrApplyRequestDto;
import com.feple.feple_backend.admin.ocr.OcrApplyResultDto;
import com.feple.feple_backend.admin.ocr.OcrParseResult;
import com.feple.feple_backend.admin.ocr.OcrResultDto;
import com.feple.feple_backend.admin.ocr.TimetableOcrService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TimetableOcrAdminControllerTest {

    @Mock TimetableOcrService ocrService;
    @Mock AdminLogService adminLogService;

    @InjectMocks TimetableOcrAdminController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
        given(ocrService.parseTimetable(any(), any())).willReturn(new OcrParseResult<>(List.of(), false));

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
}
