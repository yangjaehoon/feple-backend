package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artist.service.ArtistService;
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
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ArtistAdminControllerTest {

    @Mock ArtistService artistService;
    @Mock ArtistAdminService artistAdminService;
    @Mock ArtistSuggestionAdminService artistSuggestionAdminService;
    @Mock AdminLogService adminLogService;

    @InjectMocks ArtistAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/artists ────────────────────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(artistAdminService.getAdminArtistList(anyString(), anyString(), any(), anyInt()))
                .willReturn(new PageImpl<>(List.of()));
        given(artistSuggestionAdminService.getPendingSuggestionsPreview(anyInt())).willReturn(List.of());
        given(artistSuggestionAdminService.getProcessedSuggestionsPreview(anyInt())).willReturn(List.of());
        given(artistSuggestionAdminService.getProcessedCount()).willReturn(0L);

        mockMvc.perform(get("/admin/artists"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/list"))
                .andExpect(model().attributeExists("artistsPage", "artists", "keyword", "sort",
                        "allGenres", "suggestions"));
    }

    @Test
    void 장르_필터_있으면_getAdminArtistList에_전달() throws Exception {
        given(artistAdminService.getAdminArtistList(anyString(), anyString(), eq(ArtistGenre.INDIE), anyInt()))
                .willReturn(new PageImpl<>(List.of()));
        given(artistSuggestionAdminService.getPendingSuggestionsPreview(anyInt())).willReturn(List.of());
        given(artistSuggestionAdminService.getProcessedSuggestionsPreview(anyInt())).willReturn(List.of());
        given(artistSuggestionAdminService.getProcessedCount()).willReturn(0L);

        mockMvc.perform(get("/admin/artists").param("genre", "INDIE"))
                .andExpect(status().isOk());

        then(artistAdminService).should().getAdminArtistList(anyString(), anyString(), eq(ArtistGenre.INDIE), anyInt());
    }

    // ── GET /admin/artists/new ────────────────────────────────────────────────

    @Test
    void 신규_아티스트_폼_조회() throws Exception {
        mockMvc.perform(get("/admin/artists/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/create"))
                .andExpect(model().attributeExists("artist"));
    }

    // ── GET /admin/artists/{id}/edit ──────────────────────────────────────────

    @Test
    void 편집_폼_조회_성공() throws Exception {
        given(artistAdminService.getArtistForEdit(1L)).willReturn(mock(ArtistRequestDto.class));

        mockMvc.perform(get("/admin/artists/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/edit"))
                .andExpect(model().attributeExists("artistId", "artist", "page"));
    }

    @Test
    void 편집_폼_조회_없는_아티스트_목록으로_리다이렉트() throws Exception {
        given(artistAdminService.getArtistForEdit(99L)).willThrow(new NoSuchElementException("없는 아티스트"));

        mockMvc.perform(get("/admin/artists/99/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/artists"))
                .andExpect(flash().attribute("errorMessage", "없는 아티스트"));
    }

    // ── POST /admin/artists/{id}/edit ─────────────────────────────────────────

    @Test
    void 아티스트_수정_성공() throws Exception {
        mockMvc.perform(multipart("/admin/artists/1/edit")
                        .file("profileImageFile", new byte[0])
                        .param("name", "수정된아티스트")
                        .param("genres", "INDIE")
                        .param("page", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/artists?page=0"))
                .andExpect(flash().attribute("successMessage", "아티스트 정보가 수정되었습니다."));
    }

    @Test
    void 아티스트_수정_NoSuchElementException_errorMessage_설정() throws Exception {
        willThrow(new NoSuchElementException("없는 아티스트")).given(artistAdminService).updateArtist(anyLong(), any());

        mockMvc.perform(multipart("/admin/artists/1/edit")
                        .file("profileImageFile", new byte[0])
                        .param("name", "수정된아티스트")
                        .param("genres", "INDIE"))
                .andExpect(flash().attribute("errorMessage", "없는 아티스트"));
    }

    // ── POST /admin/artists/{id}/delete ──────────────────────────────────────

    @Test
    void 아티스트_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/artists/1/delete"))
                .andExpect(redirectedUrl("/admin/artists"))
                .andExpect(flash().attribute("successMessage", "아티스트가 삭제되었습니다."));

        then(artistAdminService).should().deleteArtist(1L);
    }

    @Test
    void 아티스트_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("삭제 실패")).given(artistAdminService).deleteArtist(anyLong());

        mockMvc.perform(post("/admin/artists/1/delete"))
                .andExpect(flash().attribute("errorMessage", "삭제 중 오류가 발생했습니다."));
    }

    // ── POST /admin/artists/suggestions/{id}/dismiss ──────────────────────────

    @Test
    void 아티스트_신청_기각_성공() throws Exception {
        mockMvc.perform(post("/admin/artists/suggestions/1/dismiss")
                        .param("processNote", "중복 아티스트"))
                .andExpect(redirectedUrl("/admin/artists"))
                .andExpect(flash().attribute("successMessage", "아티스트 신청이 처리되었습니다."));

        then(artistSuggestionAdminService).should().dismiss(1L, "중복 아티스트");
    }

    // ── POST /admin/artists/batch-name-en ─────────────────────────────────────

    @Test
    void 영어_이름_일괄_수정_성공() throws Exception {
        mockMvc.perform(post("/admin/artists/batch-name-en")
                        .param("artistIds", "1", "2")
                        .param("nameEns", "Artist A", "Artist B"))
                .andExpect(redirectedUrl("/admin/artists"))
                .andExpect(flash().attribute("successMessage", "영어 이름이 저장되었습니다."));

        then(artistAdminService).should().batchUpdateNameEn(List.of(1L, 2L), List.of("Artist A", "Artist B"));
    }
}
