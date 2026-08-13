package com.feple.feple_backend.admin.artist;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.ocr.UnmatchedArtistSuggestionService;
import com.feple.feple_backend.artist.dto.ArtistAdminListQuery;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import com.feple.feple_backend.global.MusicGenre;
import java.util.List;
import java.util.NoSuchElementException;
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
class ArtistAdminControllerTest {

    @Mock ArtistService artistService;
    @Mock ArtistAdminService artistAdminService;
    @Mock ArtistSuggestionAdminService artistSuggestionAdminService;
    @Mock UnmatchedArtistSuggestionService unmatchedArtistSuggestionService;
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
        given(artistAdminService.getAdminArtistList(any(ArtistAdminListQuery.class)))
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
        given(artistAdminService.getAdminArtistList(eq(new ArtistAdminListQuery("", "", MusicGenre.INDIE, 0))))
                .willReturn(new PageImpl<>(List.of()));
        given(artistSuggestionAdminService.getPendingSuggestionsPreview(anyInt())).willReturn(List.of());
        given(artistSuggestionAdminService.getProcessedSuggestionsPreview(anyInt())).willReturn(List.of());
        given(artistSuggestionAdminService.getProcessedCount()).willReturn(0L);

        mockMvc.perform(get("/admin/artists").param("genre", "INDIE"))
                .andExpect(status().isOk());

        then(artistAdminService).should().getAdminArtistList(eq(new ArtistAdminListQuery("", "", MusicGenre.INDIE, 0)));
    }

    // ── GET /admin/artists/new ────────────────────────────────────────────────

    @Test
    void 신규_아티스트_폼_조회() throws Exception {
        mockMvc.perform(get("/admin/artists/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/create"))
                .andExpect(model().attributeExists("artist"));
    }

    @Test
    void 신규_아티스트_폼_조회시_name_파라미터_있으면_트림후_반영() throws Exception {
        mockMvc.perform(get("/admin/artists/new").param("name", "  아이유  "))
                .andExpect(status().isOk())
                .andExpect(model().attribute("artist",
                        org.hamcrest.Matchers.hasProperty("name", org.hamcrest.Matchers.equalTo("아이유"))));
    }

    @Test
    void 신규_아티스트_폼_조회시_suggestionId_모델에_반영() throws Exception {
        mockMvc.perform(get("/admin/artists/new").param("suggestionId", "7"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("suggestionId", 7L));
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
    void 아티스트_수정_성공시_리다이렉트_URL의_한글_키워드가_인코딩됨() throws Exception {
        // encode() 없이 build()만 하면 keyword의 한글이 그대로 Location 헤더에 들어가
        // Tomcat이 "invalid header"로 판단해 리다이렉트 자체를 제거해버렸다(빈 화면 원인) — 회귀 테스트
        mockMvc.perform(multipart("/admin/artists/1/edit")
                        .file("profileImageFile", new byte[0])
                        .param("name", "수정된아티스트")
                        .param("genres", "INDIE")
                        .param("page", "0")
                        .param("keyword", "아시안"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/artists?page=0&keyword=%EC%95%84%EC%8B%9C%EC%95%88"));
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

    @Test
    void 아티스트_수정_검증오류시_edit_뷰_재표시() throws Exception {
        mockMvc.perform(multipart("/admin/artists/1/edit")
                        .file("profileImageFile", new byte[0])
                        .param("name", "")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/edit"))
                .andExpect(model().attributeExists("errors", "artistId"));

        then(artistAdminService).should(never()).updateArtist(anyLong(), any());
    }

    @Test
    void 아티스트_수정시_새_프로필이미지_있으면_업로드후_반영() throws Exception {
        given(artistAdminService.uploadProfile(any(), any())).willReturn("new-key.jpg");

        mockMvc.perform(multipart("/admin/artists/1/edit")
                        .file("profileImageFile", new byte[]{1, 2, 3})
                        .param("name", "수정된아티스트")
                        .param("genres", "INDIE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "아티스트 정보가 수정되었습니다."));

        then(artistAdminService).should().uploadProfile(any(), eq("수정된아티스트"));
    }

    @Test
    void 아티스트_수정_일반_예외시_고정_에러메시지() throws Exception {
        willThrow(new RuntimeException("DB 오류")).given(artistAdminService).updateArtist(anyLong(), any());

        mockMvc.perform(multipart("/admin/artists/1/edit")
                        .file("profileImageFile", new byte[0])
                        .param("name", "수정된아티스트")
                        .param("genres", "INDIE"))
                .andExpect(flash().attribute("errorMessage", "수정 중 오류가 발생했습니다."));
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

    // ── POST /admin/artists/new ────────────────────────────────────────────────

    @Test
    void 아티스트_등록_성공() throws Exception {
        given(artistAdminService.createArtist(any())).willReturn(1L);

        mockMvc.perform(multipart("/admin/artists/new")
                        .file("profileImageFile", new byte[]{1, 2, 3})
                        .param("name", "새아티스트")
                        .param("genres", "INDIE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/artists"))
                .andExpect(flash().attribute("successMessage", "'새아티스트' 아티스트가 등록되었습니다."));

        then(artistAdminService).should().createArtist(any());
    }

    @Test
    void 아티스트_등록시_suggestionId_있으면_해당_신청_자동_승인() throws Exception {
        given(artistAdminService.createArtist(any())).willReturn(5L);

        mockMvc.perform(multipart("/admin/artists/new")
                        .file("profileImageFile", new byte[]{1, 2, 3})
                        .param("name", "새아티스트")
                        .param("genres", "INDIE")
                        .param("suggestionId", "7"))
                .andExpect(status().is3xxRedirection());

        then(artistSuggestionAdminService).should().approve(7L, 5L);
    }

    @Test
    void 아티스트_등록시_unmatchedSuggestionId_있으면_해당_제안_자동_삭제() throws Exception {
        given(artistAdminService.createArtist(any())).willReturn(5L);

        mockMvc.perform(multipart("/admin/artists/new")
                        .file("profileImageFile", new byte[]{1, 2, 3})
                        .param("name", "새아티스트")
                        .param("genres", "INDIE")
                        .param("unmatchedSuggestionId", "9"))
                .andExpect(status().is3xxRedirection());

        then(unmatchedArtistSuggestionService).should().delete(9L);
    }

    @Test
    void 아티스트_등록시_신청_자동_승인_실패해도_등록_자체는_성공() throws Exception {
        given(artistAdminService.createArtist(any())).willReturn(5L);
        willThrow(new IllegalArgumentException("이미 처리된 아티스트 신청입니다."))
                .given(artistSuggestionAdminService).approve(anyLong(), anyLong());

        mockMvc.perform(multipart("/admin/artists/new")
                        .file("profileImageFile", new byte[]{1, 2, 3})
                        .param("name", "새아티스트")
                        .param("genres", "INDIE")
                        .param("suggestionId", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/artists"))
                .andExpect(flash().attribute("successMessage", "'새아티스트' 아티스트가 등록되었습니다."));
    }

    @Test
    void 아티스트_등록_프로필이미지_없으면_검증오류() throws Exception {
        mockMvc.perform(multipart("/admin/artists/new")
                        .param("name", "새아티스트")
                        .param("genres", "INDIE"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/create"))
                .andExpect(model().attributeExists("errors"));

        then(artistAdminService).should(never()).createArtist(any());
    }

    @Test
    void 아티스트_등록_실패시_오류메시지() throws Exception {
        given(artistAdminService.uploadProfile(any(), any())).willThrow(new RuntimeException("업로드 실패"));

        mockMvc.perform(multipart("/admin/artists/new")
                        .file("profileImageFile", new byte[]{1, 2, 3})
                        .param("name", "새아티스트")
                        .param("genres", "INDIE"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/create"))
                .andExpect(model().attributeExists("errors"));
    }

    // ── GET /admin/artists/photos ─────────────────────────────────────────────

    @Test
    void 사진관리_뷰와_모델() throws Exception {
        given(artistAdminService.getAllArtistsSortedByName()).willReturn(List.of());

        mockMvc.perform(get("/admin/artists/photos"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/photos"))
                .andExpect(model().attributeExists("artists"));
    }

    // ── POST /admin/artists/{id}/photo ─────────────────────────────────────────

    @Test
    void 사진_업로드_파일없으면_에러메시지() throws Exception {
        mockMvc.perform(multipart("/admin/artists/1/photo")
                        .file("profileImageFile", new byte[0]))
                .andExpect(redirectedUrl("/admin/artists/photos"))
                .andExpect(flash().attribute("errorMessage", "이미지를 선택해주세요."));
    }

    @Test
    void 사진_업로드_성공() throws Exception {
        com.feple.feple_backend.artist.dto.ArtistResponseDto artist =
                mock(com.feple.feple_backend.artist.dto.ArtistResponseDto.class);
        given(artist.getName()).willReturn("아티스트명");
        given(artistService.getArtistById(1L)).willReturn(artist);
        given(artistAdminService.uploadProfile(any(), any())).willReturn("key.jpg");

        mockMvc.perform(multipart("/admin/artists/1/photo")
                        .file("profileImageFile", new byte[]{1, 2, 3}))
                .andExpect(redirectedUrl("/admin/artists/photos"))
                .andExpect(flash().attribute("successMessage", "사진이 업데이트되었습니다."));

        then(artistAdminService).should().updateArtistPhoto(1L, "key.jpg");
    }

    @Test
    void 사진_업로드_실패시_에러메시지() throws Exception {
        given(artistService.getArtistById(1L)).willThrow(new RuntimeException("조회 실패"));

        mockMvc.perform(multipart("/admin/artists/1/photo")
                        .file("profileImageFile", new byte[]{1, 2, 3}))
                .andExpect(redirectedUrl("/admin/artists/photos"))
                .andExpect(flash().attribute("errorMessage", "사진 업로드에 실패했습니다. 다시 시도해주세요."));
    }

    // ── GET /admin/artists/deleted ───────────────────────────────────────────

    @Test
    void 삭제된_아티스트_목록_조회() throws Exception {
        given(artistAdminService.getDeletedArtists()).willReturn(List.of());

        mockMvc.perform(get("/admin/artists/deleted"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/artist/deleted"))
                .andExpect(model().attributeExists("artists"));
    }

    // ── POST /admin/artists/{id}/restore ─────────────────────────────────────

    @Test
    void 아티스트_복구_성공() throws Exception {
        mockMvc.perform(post("/admin/artists/1/restore"))
                .andExpect(redirectedUrl("/admin/artists/deleted"))
                .andExpect(flash().attribute("successMessage", "아티스트가 복구되었습니다."));

        then(artistAdminService).should().restoreArtist(1L);
    }
}
