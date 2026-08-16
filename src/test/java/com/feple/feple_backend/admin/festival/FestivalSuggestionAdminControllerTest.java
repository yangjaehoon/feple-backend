package com.feple.feple_backend.admin.festival;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.festival.suggestion.service.FestivalSuggestionAdminService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FestivalSuggestionAdminControllerTest {

    @Mock FestivalSuggestionAdminService festivalSuggestionAdminService;
    @Mock FestivalAdminService festivalAdminService;
    @Mock AdminLogService adminLogService;

    @InjectMocks FestivalSuggestionAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/festival-suggestions ─────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(festivalSuggestionAdminService.getSuggestionsPage(anyInt(), anyInt()))
                .willReturn(new PageImpl<>(List.of()));
        given(festivalAdminService.getAllFestivalsForAdmin()).willReturn(List.of());

        mockMvc.perform(get("/admin/festival-suggestions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/suggestions"))
                .andExpect(model().attributeExists("suggestions", "allFestivals"));
    }

    // ── POST /admin/festival-suggestions/{id}/approve ───────────────────────

    @Test
    void 승인_성공() throws Exception {
        mockMvc.perform(post("/admin/festival-suggestions/1/approve")
                        .param("festivalId", "100"))
                .andExpect(redirectedUrl("/admin/festival-suggestions"))
                .andExpect(flash().attribute("successMessage", "페스티벌 신청이 승인되었습니다."));

        then(festivalSuggestionAdminService).should().approve(1L, 100L);
    }

    @Test
    void 승인_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(festivalSuggestionAdminService).approve(anyLong(), anyLong());

        mockMvc.perform(post("/admin/festival-suggestions/1/approve")
                        .param("festivalId", "100"))
                .andExpect(flash().attribute("errorMessage", "승인 중 오류가 발생했습니다."));
    }

    // ── POST /admin/festival-suggestions/{id}/dismiss ───────────────────────

    @Test
    void 기각_성공_processNote_전달() throws Exception {
        mockMvc.perform(post("/admin/festival-suggestions/1/dismiss")
                        .param("processNote", "중복 신청"))
                .andExpect(redirectedUrl("/admin/festival-suggestions"))
                .andExpect(flash().attribute("successMessage", "페스티벌 신청이 처리되었습니다."));

        then(festivalSuggestionAdminService).should().dismiss(1L, "중복 신청");
    }

    @Test
    void 기각_빈_processNote는_null로_전달() throws Exception {
        mockMvc.perform(post("/admin/festival-suggestions/1/dismiss")
                        .param("processNote", ""))
                .andExpect(redirectedUrl("/admin/festival-suggestions"));

        then(festivalSuggestionAdminService).should().dismiss(1L, null);
    }

    @Test
    void 기각_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(festivalSuggestionAdminService).dismiss(anyLong(), any());

        mockMvc.perform(post("/admin/festival-suggestions/1/dismiss"))
                .andExpect(flash().attribute("errorMessage", "처리 중 오류가 발생했습니다."));
    }
}
