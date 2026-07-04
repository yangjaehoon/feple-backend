package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.checklist.FestivalChecklistService;
import com.feple.feple_backend.admin.festival.FestivalDetailAggregationService;
import com.feple.feple_backend.admin.festival.FestivalDetailDto;
import com.feple.feple_backend.admin.festival.FestivalRatingStatsDto;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.booth.entity.BoothType;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
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
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FestivalAdminControllerTest {

    @Mock FestivalService festivalService;
    @Mock ArtistAdminService artistService;
    @Mock ArtistFestivalService artistFestivalService;
    @Mock FestivalDetailAggregationService festivalDetailAggregationService;
    @Mock FestivalChecklistService festivalChecklistService;
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

    // ── GET /admin/festivals ──────────────────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(festivalService.getFestivalsAdminPage(anyString(), anyInt(), anyInt()))
                .willReturn(new PageImpl<>(List.of()));
        given(festivalService.getAllActiveFestivalsForAdmin()).willReturn(List.of());
        given(festivalChecklistService.getChecklistMap(any())).willReturn(Map.of());

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
    void 페스티벌_삭제_성공_successMessage_없음() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/delete"))
                .andExpect(redirectedUrl("/admin/festivals"))
                .andExpect(flash().attributeCount(0));

        then(festivalService).should().deleteFestival(1L);
    }

    @Test
    void 페스티벌_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("삭제 실패")).given(festivalService).deleteFestival(anyLong());

        mockMvc.perform(post("/admin/festivals/1/delete"))
                .andExpect(flash().attribute("errorMessage", "삭제 중 오류가 발생했습니다."));
    }
}
