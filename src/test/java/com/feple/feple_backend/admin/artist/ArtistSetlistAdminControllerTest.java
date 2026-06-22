package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artist.song.service.SongService;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ArtistSetlistAdminControllerTest {

    @Mock SongService songService;
    @Mock SongAdminService songAdminService;
    @Mock ArtistService artistService;
    @Mock ArtistFestivalService artistFestivalService;

    @InjectMocks ArtistSetlistAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/artists/{artistId}/setlist ──────────────────────────────────

    @Test
    void 셋리스트_목록_조회_성공() throws Exception {
        given(artistService.getArtistById(1L)).willReturn(mock(ArtistResponseDto.class));
        given(artistFestivalService.getAppearancesByArtistId(1L)).willReturn(List.of());

        mockMvc.perform(get("/admin/artists/1/setlist"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/setlist/index"))
                .andExpect(model().attributeExists("artist", "appearances"));
    }

    @Test
    void 셋리스트_목록_조회_NoSuchElement_목록으로_리다이렉트() throws Exception {
        given(artistService.getArtistById(99L)).willThrow(new NoSuchElementException("없는 아티스트"));

        mockMvc.perform(get("/admin/artists/99/setlist"))
                .andExpect(redirectedUrl("/admin/artists"))
                .andExpect(flash().attribute("errorMessage", "없는 아티스트"));
    }

    @Test
    void 셋리스트_목록_조회_RuntimeException_일반_에러메시지() throws Exception {
        given(artistService.getArtistById(1L)).willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/admin/artists/1/setlist"))
                .andExpect(redirectedUrl("/admin/artists"))
                .andExpect(flash().attribute("errorMessage", "정보를 불러오는 중 오류가 발생했습니다."));
    }

    // ── GET /admin/artists/{artistId}/setlist/{artistFestivalId} ──────────────

    @Test
    void 셋리스트_편집_조회_성공() throws Exception {
        given(artistService.getArtistById(1L)).willReturn(mock(ArtistResponseDto.class));
        given(artistFestivalService.getArtistFestivalByIdAndArtistId(2L, 1L))
                .willReturn(mock(ArtistFestival.class));
        given(songAdminService.getSetlist(2L)).willReturn(List.of());
        given(songService.getSongsByArtistId(1L)).willReturn(List.of());

        mockMvc.perform(get("/admin/artists/1/setlist/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/setlist/edit"))
                .andExpect(model().attributeExists("artist", "artistFestival", "songs", "selectedSongIds"));
    }

    @Test
    void 셋리스트_편집_조회_IllegalArgument_셋리스트_목록으로_리다이렉트() throws Exception {
        given(artistService.getArtistById(1L)).willReturn(mock(ArtistResponseDto.class));
        given(artistFestivalService.getArtistFestivalByIdAndArtistId(2L, 1L))
                .willThrow(new IllegalArgumentException("잘못된 접근"));

        mockMvc.perform(get("/admin/artists/1/setlist/2"))
                .andExpect(redirectedUrl("/admin/artists/1/setlist"))
                .andExpect(flash().attribute("errorMessage", "잘못된 접근"));
    }

    @Test
    void 셋리스트_편집_조회_NoSuchElement_목록으로_리다이렉트() throws Exception {
        given(artistService.getArtistById(1L)).willThrow(new NoSuchElementException("없는 아티스트"));

        mockMvc.perform(get("/admin/artists/1/setlist/2"))
                .andExpect(redirectedUrl("/admin/artists"));
    }

    // ── POST /admin/artists/{artistId}/setlist/{artistFestivalId} ─────────────

    @Test
    void 셋리스트_저장_아티스트_불일치_errorMessage() throws Exception {
        given(artistFestivalService.existsByIdAndArtistId(2L, 1L)).willReturn(false);

        mockMvc.perform(post("/admin/artists/1/setlist/2"))
                .andExpect(redirectedUrl("/admin/artists/1/setlist"))
                .andExpect(flash().attribute("errorMessage", "해당 아티스트의 셋리스트가 아닙니다."));
    }

    @Test
    void 셋리스트_저장_성공() throws Exception {
        given(artistFestivalService.existsByIdAndArtistId(2L, 1L)).willReturn(true);

        mockMvc.perform(post("/admin/artists/1/setlist/2")
                        .param("songIds", "10", "20"))
                .andExpect(redirectedUrl("/admin/artists/1/setlist"))
                .andExpect(flash().attribute("successMessage", "셋리스트가 저장되었습니다."));
    }

    @Test
    void 셋리스트_저장_festivalId있으면_페스티벌_셋리스트로_리다이렉트() throws Exception {
        given(artistFestivalService.existsByIdAndArtistId(2L, 1L)).willReturn(true);

        mockMvc.perform(post("/admin/artists/1/setlist/2")
                        .param("festivalId", "5"))
                .andExpect(redirectedUrl("/admin/festivals/5#setlist"));
    }
}
