package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ArtistSuggestionAdminControllerTest {

    @Mock ArtistSuggestionAdminService artistSuggestionAdminService;
    @Mock AdminLogService adminLogService;

    @InjectMocks ArtistSuggestionAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/artist-suggestions ─────────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(artistSuggestionAdminService.getSuggestionsPage(anyInt(), anyInt()))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/artist-suggestions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/suggestions"))
                .andExpect(model().attributeExists("suggestions"));
    }

    // ── POST /admin/artist-suggestions/{id}/dismiss ───────────────────────────

    @Test
    void 기각_성공_processNote_전달() throws Exception {
        mockMvc.perform(post("/admin/artist-suggestions/1/dismiss")
                        .param("processNote", "중복 아티스트"))
                .andExpect(redirectedUrl("/admin/artist-suggestions"))
                .andExpect(flash().attribute("successMessage", "아티스트 신청이 처리되었습니다."));

        then(artistSuggestionAdminService).should().dismiss(1L, "중복 아티스트");
    }

    @Test
    void 기각_빈_processNote는_null로_전달() throws Exception {
        mockMvc.perform(post("/admin/artist-suggestions/1/dismiss")
                        .param("processNote", ""))
                .andExpect(redirectedUrl("/admin/artist-suggestions"));

        then(artistSuggestionAdminService).should().dismiss(1L, null);
    }

    @Test
    void 기각_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(artistSuggestionAdminService).dismiss(anyLong(), any());

        mockMvc.perform(post("/admin/artist-suggestions/1/dismiss"))
                .andExpect(flash().attribute("errorMessage", "처리 중 오류가 발생했습니다."));
    }
}
