package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artist.song.service.SongRequestAdminService;
import com.feple.feple_backend.artist.song.service.SongService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ArtistSongAdminControllerTest {

    @Mock SongService songService;
    @Mock SongAdminService songAdminService;
    @Mock ArtistService artistService;
    @Mock SongRequestAdminService songRequestAdminService;

    @InjectMocks ArtistSongAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/artists/{artistId}/songs ────────────────────────────────────

    @Test
    void 곡_목록_조회_성공() throws Exception {
        given(artistService.getArtistById(1L)).willReturn(mock(ArtistResponseDto.class));
        given(songService.getSongsByArtistId(1L)).willReturn(List.of());
        given(songRequestAdminService.getPendingRequests(1L)).willReturn(List.of());

        mockMvc.perform(get("/admin/artists/1/songs"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/songs"))
                .andExpect(model().attributeExists("artist", "songs", "pendingRequests"));
    }

    @Test
    void 곡_목록_조회_예외_목록으로_리다이렉트() throws Exception {
        given(artistService.getArtistById(1L)).willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/admin/artists/1/songs"))
                .andExpect(redirectedUrl("/admin/artists"))
                .andExpect(flash().attribute("errorMessage", "곡 목록을 불러오는 중 오류가 발생했습니다."));
    }

    // ── POST /admin/artists/{artistId}/songs ───────────────────────────────────

    @Test
    void 곡_등록_검증실패_errorMessage_설정() throws Exception {
        // title만 있고 @NotBlank youtubeVideoId 없음
        mockMvc.perform(post("/admin/artists/1/songs")
                        .param("title", "테스트 곡"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(redirectedUrl("/admin/artists/1/songs"));
    }

    @Test
    void 곡_등록_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/artists/1/songs")
                        .param("youtubeVideoId", "abc123")
                        .param("title", "테스트 곡"))
                .andExpect(redirectedUrl("/admin/artists/1/songs"))
                .andExpect(flash().attribute("successMessage", "'테스트 곡' 곡이 등록되었습니다."));
    }

    @Test
    void 곡_등록_서비스_예외_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(songAdminService).saveSong(anyLong(), any());

        mockMvc.perform(post("/admin/artists/1/songs")
                        .param("youtubeVideoId", "abc123")
                        .param("title", "테스트 곡"))
                .andExpect(flash().attribute("errorMessage", "곡 등록 중 오류가 발생했습니다."));
    }

    // ── POST /admin/artists/{artistId}/songs/{songId}/delete ──────────────────

    @Test
    void 곡_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/artists/1/songs/10/delete"))
                .andExpect(redirectedUrl("/admin/artists/1/songs"))
                .andExpect(flash().attribute("successMessage", "곡이 삭제되었습니다."));

        then(songAdminService).should().deleteSong(1L, 10L);
    }

    @Test
    void 곡_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(songAdminService).deleteSong(anyLong(), anyLong());

        mockMvc.perform(post("/admin/artists/1/songs/10/delete"))
                .andExpect(flash().attribute("errorMessage", "삭제 중 오류가 발생했습니다."));
    }

    // ── POST /admin/artists/{artistId}/songs/song-requests/{requestId}/approve ─

    @Test
    void 노래_요청_승인_곡_등록됨_successMessage() throws Exception {
        given(songRequestAdminService.approve(1L, null)).willReturn(true);

        mockMvc.perform(post("/admin/artists/1/songs/song-requests/1/approve"))
                .andExpect(redirectedUrl("/admin/artists/1/songs"))
                .andExpect(flash().attribute("successMessage",
                        "노래 요청이 승인되었습니다. 곡이 등록되었습니다."));
    }

    @Test
    void 노래_요청_승인_youtubeUrl있지만_곡_미등록_successMessage() throws Exception {
        given(songRequestAdminService.approve(1L, "https://youtu.be/abc")).willReturn(false);

        mockMvc.perform(post("/admin/artists/1/songs/song-requests/1/approve")
                        .param("youtubeUrl", "https://youtu.be/abc"))
                .andExpect(flash().attribute("successMessage",
                        "노래 요청이 승인되었습니다. (YouTube 영상 정보를 가져오지 못해 곡은 등록되지 않았습니다.)"));
    }

    @Test
    void 노래_요청_승인_url없이_곡_미등록_successMessage() throws Exception {
        given(songRequestAdminService.approve(1L, null)).willReturn(false);

        mockMvc.perform(post("/admin/artists/1/songs/song-requests/1/approve"))
                .andExpect(flash().attribute("successMessage", "노래 요청이 승인되었습니다."));
    }

    @Test
    void 노래_요청_승인_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(songRequestAdminService).approve(anyLong(), any());

        mockMvc.perform(post("/admin/artists/1/songs/song-requests/1/approve"))
                .andExpect(flash().attribute("errorMessage", "노래 요청 승인에 실패했습니다. 다시 시도해주세요."));
    }

    // ── POST /admin/artists/{artistId}/songs/song-requests/{requestId}/reject ──

    @Test
    void 노래_요청_거절_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/artists/1/songs/song-requests/1/reject")
                        .param("reason", "부적절"))
                .andExpect(redirectedUrl("/admin/artists/1/songs"))
                .andExpect(flash().attribute("successMessage", "노래 요청이 거절되었습니다."));

        then(songRequestAdminService).should().reject(1L, "부적절");
    }

    @Test
    void 노래_요청_거절_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(songRequestAdminService).reject(anyLong(), any());

        mockMvc.perform(post("/admin/artists/1/songs/song-requests/1/reject"))
                .andExpect(flash().attribute("errorMessage", "노래 요청 거절에 실패했습니다. 다시 시도해주세요."));
    }
}
