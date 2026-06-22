package com.feple.feple_backend.admin.song;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.song.service.SongRequestAdminService;
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
class SongRequestAdminControllerTest {

    @Mock SongRequestAdminService songRequestAdminService;
    @Mock AdminLogService adminLogService;

    @InjectMocks SongRequestAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/song-requests ──────────────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(songRequestAdminService.getRequestsPage(anyInt(), anyInt(), anyString(), anyString()))
                .willReturn(new PageImpl<>(List.of()));
        given(songRequestAdminService.getPendingCount()).willReturn(0L);

        mockMvc.perform(get("/admin/song-requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/song-request/list"))
                .andExpect(model().attributeExists("requests", "status", "keyword", "pendingCount"));
    }

    // ── POST /admin/song-requests/{id}/approve ────────────────────────────────

    @Test
    void 승인_성공_노래_등록됨() throws Exception {
        given(songRequestAdminService.approve(1L, null)).willReturn(true);

        mockMvc.perform(post("/admin/song-requests/1/approve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "노래 요청이 승인되었습니다. 곡이 등록되었습니다."));
    }

    @Test
    void 승인_성공_노래_미등록_youtubeUrl_없음() throws Exception {
        given(songRequestAdminService.approve(1L, null)).willReturn(false);

        mockMvc.perform(post("/admin/song-requests/1/approve"))
                .andExpect(flash().attribute("successMessage", "노래 요청이 승인되었습니다."));
    }

    @Test
    void 승인_성공_노래_미등록_youtubeUrl_있음() throws Exception {
        given(songRequestAdminService.approve(1L, "https://youtu.be/abc")).willReturn(false);

        mockMvc.perform(post("/admin/song-requests/1/approve")
                        .param("youtubeUrl", "https://youtu.be/abc"))
                .andExpect(flash().attribute("successMessage",
                        "승인되었습니다. (YouTube 영상 정보를 가져오지 못해 곡은 등록되지 않았습니다.)"));
    }

    @Test
    void 승인_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(songRequestAdminService).approve(anyLong(), any());

        mockMvc.perform(post("/admin/song-requests/1/approve"))
                .andExpect(flash().attribute("errorMessage", "승인 처리 중 오류가 발생했습니다."));
    }

    @Test
    void 승인_후_listRedirect_반환() throws Exception {
        given(songRequestAdminService.approve(1L, null)).willReturn(true);

        mockMvc.perform(post("/admin/song-requests/1/approve")
                        .param("status", "PENDING").param("page", "0").param("keyword", ""))
                .andExpect(redirectedUrl("/admin/song-requests?status=PENDING&page=0"));
    }

    // ── POST /admin/song-requests/{id}/reject ─────────────────────────────────

    @Test
    void 거절_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/song-requests/1/reject"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "노래 요청이 거절되었습니다."));

        then(songRequestAdminService).should().reject(1L, null);
    }

    @Test
    void 거절_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(songRequestAdminService).reject(anyLong(), any());

        mockMvc.perform(post("/admin/song-requests/1/reject"))
                .andExpect(flash().attribute("errorMessage", "거절 처리 중 오류가 발생했습니다."));
    }
}
