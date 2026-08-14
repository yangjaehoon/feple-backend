package com.feple.feple_backend.admin.festival;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.admin.checklist.FestivalChecklistService;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.booth.entity.BoothType;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.festival.suggestion.service.FestivalSuggestionAdminService;
import java.util.List;
import java.util.Map;
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
class FestivalAdminControllerTest {

    @Mock FestivalAdminService festivalService;
    @Mock ArtistAdminService artistService;
    @Mock ArtistFestivalService artistFestivalService;
    @Mock FestivalDetailAggregationService festivalDetailAggregationService;
    @Mock FestivalChecklistService festivalChecklistService;
    @Mock FestivalSuggestionAdminService festivalSuggestionAdminService;
    @Mock AdminLogService adminLogService;

    @InjectMocks FestivalAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/festivals/new ──────────────────────────────────────────────

    @Test
    void 신규_페스티벌_폼_조회() throws Exception {
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());

        mockMvc.perform(get("/admin/festivals/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/create"))
                .andExpect(model().attributeExists("festival", "allArtists", "allRegions",
                        "allGenres", "allAgeRestrictions"));
    }

    @Test
    void 신규_페스티벌_폼_조회시_suggestionId와_이름_모델에_반영() throws Exception {
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());

        mockMvc.perform(get("/admin/festivals/new")
                        .param("name", "신청된 페스티벌")
                        .param("suggestionId", "7"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("suggestionId", 7L));
    }

    // ── GET /admin/festivals ──────────────────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(festivalService.getFestivalsAdminPage(anyString(), anyInt(), anyInt()))
                .willReturn(new PageImpl<>(List.of()));
        given(festivalService.getAllActiveFestivalsForAdmin()).willReturn(List.of());
        given(festivalChecklistService.getChecklistMap()).willReturn(Map.of());

        mockMvc.perform(get("/admin/festivals"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/list"))
                .andExpect(model().attributeExists("festivalsPage", "festivals", "keyword",
                        "checklistMap", "activeFestivalCount"));
    }

    // ── GET /admin/festivals/{id} ─────────────────────────────────────────────

    @Test
    void 상세_조회_성공() throws Exception {
        FestivalDetailDto detail = new FestivalDetailDto(
                mock(FestivalResponseDto.class), List.of(), List.of(), List.of(),
                Map.of(), List.of(), List.of(), BoothType.values(),
                "google-key", Map.of(), "", FestivalRatingStatsDto.EMPTY);
        given(festivalDetailAggregationService.getDetail(1L)).willReturn(detail);

        mockMvc.perform(get("/admin/festivals/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/detail"))
                .andExpect(model().attributeExists("festival", "participatingArtists",
                        "timetableEntries", "stages", "booths"));
    }

    @Test
    void 상세_조회_예외_목록으로_리다이렉트() throws Exception {
        given(festivalDetailAggregationService.getDetail(99L))
                .willThrow(new NoSuchElementException("없는 페스티벌"));

        mockMvc.perform(get("/admin/festivals/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/festivals"))
                .andExpect(flash().attribute("errorMessage", "없는 페스티벌"));
    }

    @Test
    void 상세_조회_일반_예외_일반_에러메시지() throws Exception {
        given(festivalDetailAggregationService.getDetail(1L))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/admin/festivals/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/festivals"))
                .andExpect(flash().attribute("errorMessage", "페스티벌 정보를 불러오는 중 오류가 발생했습니다."));
    }

    // ── GET /admin/festivals/{id}/edit ────────────────────────────────────────

    @Test
    void 편집_폼_조회_성공() throws Exception {
        FestivalResponseDto festival = mock(FestivalResponseDto.class);
        given(festival.getPosterUrl()).willReturn("poster.jpg");
        given(festivalService.getFestival(1L)).willReturn(festival);
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());

        mockMvc.perform(get("/admin/festivals/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/edit"))
                .andExpect(model().attributeExists("festivalId", "festival", "currentPosterUrl"));
    }

    @Test
    void 편집_폼_조회_예외_목록으로_리다이렉트() throws Exception {
        given(festivalService.getFestival(99L))
                .willThrow(new NoSuchElementException("없는 페스티벌"));

        mockMvc.perform(get("/admin/festivals/99/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/festivals"))
                .andExpect(flash().attribute("errorMessage", "없는 페스티벌"));
    }

    // ── POST /admin/festivals/{id}/delete ─────────────────────────────────────

    @Test
    void 페스티벌_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/delete"))
                .andExpect(redirectedUrl("/admin/festivals"))
                .andExpect(flash().attribute("successMessage", "페스티벌이 삭제되었습니다."));

        then(festivalService).should().deleteFestival(1L);
    }

    @Test
    void 페스티벌_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("삭제 실패")).given(festivalService).deleteFestival(anyLong());

        mockMvc.perform(post("/admin/festivals/1/delete"))
                .andExpect(flash().attribute("errorMessage", "삭제 중 오류가 발생했습니다."));
    }

    // ── GET /admin/festivals/deleted ────────────────────────────────────────

    @Test
    void 삭제된_페스티벌_목록_조회() throws Exception {
        given(festivalService.getDeletedFestivals()).willReturn(List.of());

        mockMvc.perform(get("/admin/festivals/deleted"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/deleted"))
                .andExpect(model().attributeExists("festivals"));
    }

    // ── POST /admin/festivals/{id}/restore ──────────────────────────────────

    @Test
    void 페스티벌_복구_성공() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/restore"))
                .andExpect(redirectedUrl("/admin/festivals/deleted"))
                .andExpect(flash().attribute("successMessage", "페스티벌이 복구되었습니다."));

        then(festivalService).should().restoreFestival(1L);
    }

    // ── POST /admin/festivals/new ─────────────────────────────────────────────

    @Test
    void 페스티벌_생성_중_예기치못한_예외_생성폼_에러로_렌더링() throws Exception {
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());
        willThrow(new RuntimeException("DB 오류")).given(festivalService).createFestival(any());

        mockMvc.perform(post("/admin/festivals/new")
                        .param("title", "테스트 페스티벌")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/create"))
                .andExpect(model().attributeExists("errors"));
    }

    @Test
    void 페스티벌_생성_성공시_아티스트_연결후_상세로_리다이렉트() throws Exception {
        given(festivalService.createFestival(any())).willReturn(10L);

        mockMvc.perform(post("/admin/festivals/new")
                        .param("title", "테스트 페스티벌")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL")
                        .param("artistIds", "1", "2"))
                .andExpect(redirectedUrl("/admin/festivals/10"))
                .andExpect(flash().attribute("successMessage", "'테스트 페스티벌' 페스티벌이 등록되었습니다."));

        then(artistFestivalService).should().linkArtistsToFestival(eq(10L), eq(List.of(1L, 2L)));
        then(adminLogService).should().log(any(), eq("FESTIVAL"), eq(10L), eq("테스트 페스티벌"));
    }

    @Test
    void 페스티벌_생성시_suggestionId_있으면_해당_신청_자동_승인() throws Exception {
        given(festivalService.createFestival(any())).willReturn(10L);

        mockMvc.perform(post("/admin/festivals/new")
                        .param("title", "테스트 페스티벌")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL")
                        .param("suggestionId", "7"))
                .andExpect(status().is3xxRedirection());

        then(festivalSuggestionAdminService).should().approve(7L, 10L);
    }

    @Test
    void 페스티벌_생성시_신청_자동_승인_실패해도_등록_자체는_성공() throws Exception {
        given(festivalService.createFestival(any())).willReturn(10L);
        willThrow(new IllegalArgumentException("이미 처리된 페스티벌 신청입니다."))
                .given(festivalSuggestionAdminService).approve(anyLong(), anyLong());

        mockMvc.perform(post("/admin/festivals/new")
                        .param("title", "테스트 페스티벌")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL")
                        .param("suggestionId", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/festivals/10"))
                .andExpect(flash().attribute("successMessage", "'테스트 페스티벌' 페스티벌이 등록되었습니다."));
    }

    @Test
    void 페스티벌_생성은_성공했지만_아티스트_연결_실패시_경고메시지() throws Exception {
        given(festivalService.createFestival(any())).willReturn(10L);
        willThrow(new RuntimeException("연결 실패")).given(artistFestivalService)
                .linkArtistsToFestival(anyLong(), any());

        mockMvc.perform(post("/admin/festivals/new")
                        .param("title", "테스트 페스티벌")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL"))
                .andExpect(redirectedUrl("/admin/festivals/10"))
                .andExpect(flash().attribute("warningMessage",
                        "페스티벌은 등록되었으나 일부 아티스트 연결에 실패했습니다. 상세 탭에서 수동으로 추가해주세요."));
    }

    @Test
    void 페스티벌_생성시_종료일이_시작일보다_이전이면_생성폼_에러로_렌더링() throws Exception {
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());
        willThrow(new IllegalArgumentException("종료일은 시작일보다 이전일 수 없습니다."))
                .given(festivalService).createFestival(any());

        mockMvc.perform(post("/admin/festivals/new")
                        .param("title", "테스트 페스티벌")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-05")
                        .param("endDate", "2026-08-01")
                        .param("region", "SEOUL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/create"))
                .andExpect(model().attributeExists("errors"));
    }

    @Test
    void 페스티벌_생성시_포스터_업로드_유효성오류면_생성폼_에러로_렌더링() throws Exception {
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());
        given(festivalService.uploadPosterFile(any(), any()))
                .willThrow(new IllegalArgumentException("이미지 형식이 아닙니다."));

        mockMvc.perform(multipart("/admin/festivals/new")
                        .file("posterFile", new byte[]{1, 2, 3})
                        .param("title", "테스트 페스티벌")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/create"))
                .andExpect(model().attributeExists("errors"));

        then(festivalService).should(never()).createFestival(any());
    }

    @Test
    void 페스티벌_생성시_포스터_업로드_중_예기치못한_오류면_생성폼_에러로_렌더링() throws Exception {
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());
        given(festivalService.uploadPosterFile(any(), any()))
                .willThrow(new RuntimeException("S3 오류"));

        mockMvc.perform(multipart("/admin/festivals/new")
                        .file("posterFile", new byte[]{1, 2, 3})
                        .param("title", "테스트 페스티벌")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/create"))
                .andExpect(model().attributeExists("errors"));
    }

    // ── POST /admin/festivals/{id}/edit ───────────────────────────────────────

    @Test
    void 페스티벌_수정_성공() throws Exception {
        FestivalResponseDto festival = mock(FestivalResponseDto.class);
        given(festival.getPosterUrl()).willReturn("poster.jpg");
        given(festivalService.getFestival(1L)).willReturn(festival);

        mockMvc.perform(post("/admin/festivals/1/edit")
                        .param("title", "수정된 페스티벌")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL"))
                .andExpect(redirectedUrl("/admin/festivals/1"))
                .andExpect(flash().attribute("successMessage", "페스티벌이 수정되었습니다."));

        then(festivalService).should().updateFestival(eq(1L), any());
    }

    @Test
    void 수정시_페스티벌_없으면_목록으로_리다이렉트() throws Exception {
        given(festivalService.getFestival(99L)).willThrow(new NoSuchElementException("없는 페스티벌"));

        mockMvc.perform(post("/admin/festivals/99/edit")
                        .param("title", "수정")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL"))
                .andExpect(redirectedUrl("/admin/festivals"))
                .andExpect(flash().attribute("errorMessage", "없는 페스티벌"));

        then(festivalService).should(never()).updateFestival(any(), any());
    }

    @Test
    void 수정시_유효성오류면_편집폼_에러로_렌더링() throws Exception {
        FestivalResponseDto festival = mock(FestivalResponseDto.class);
        given(festival.getPosterUrl()).willReturn("poster.jpg");
        given(festivalService.getFestival(1L)).willReturn(festival);
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());

        mockMvc.perform(post("/admin/festivals/1/edit")
                        .param("title", "")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/edit"))
                .andExpect(model().attributeExists("errors", "currentPosterUrl"));

        then(festivalService).should(never()).updateFestival(any(), any());
    }

    @Test
    void 수정시_종료일이_시작일보다_이전이면_편집폼_에러로_렌더링() throws Exception {
        FestivalResponseDto festival = mock(FestivalResponseDto.class);
        given(festival.getPosterUrl()).willReturn("poster.jpg");
        given(festivalService.getFestival(1L)).willReturn(festival);
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());
        willThrow(new IllegalArgumentException("종료일은 시작일보다 이전일 수 없습니다."))
                .given(festivalService).updateFestival(eq(1L), any());

        mockMvc.perform(post("/admin/festivals/1/edit")
                        .param("title", "수정")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-05")
                        .param("endDate", "2026-08-01")
                        .param("region", "SEOUL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/edit"))
                .andExpect(model().attributeExists("errors"));
    }

    @Test
    void 수정중_예기치못한_예외면_errorMessage_설정후_상세로_리다이렉트() throws Exception {
        FestivalResponseDto festival = mock(FestivalResponseDto.class);
        given(festival.getPosterUrl()).willReturn("poster.jpg");
        given(festivalService.getFestival(1L)).willReturn(festival);
        willThrow(new RuntimeException("DB 오류")).given(festivalService).updateFestival(eq(1L), any());

        mockMvc.perform(post("/admin/festivals/1/edit")
                        .param("title", "수정")
                        .param("description", "설명")
                        .param("location", "서울")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-02")
                        .param("region", "SEOUL"))
                .andExpect(redirectedUrl("/admin/festivals/1"))
                .andExpect(flash().attribute("errorMessage", "수정 중 오류가 발생했습니다."));
    }

    // ── POST /admin/festivals/{id}/restore (실패) ───────────────────────────

    @Test
    void 페스티벌_복구_실패시_에러메시지() throws Exception {
        willThrow(new RuntimeException("복구 실패")).given(festivalService).restoreFestival(anyLong());

        mockMvc.perform(post("/admin/festivals/1/restore"))
                .andExpect(redirectedUrl("/admin/festivals/deleted"))
                .andExpect(flash().attribute("errorMessage", "복구 중 오류가 발생했습니다."));
    }
}
