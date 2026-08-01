package com.feple.feple_backend.admin.festival;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FestivalArtistAdminControllerTest {

    @Mock FestivalAdminService festivalService;
    @Mock ArtistAdminService artistService;
    @Mock ArtistFestivalService artistFestivalService;
    @Mock AdminLogService adminLogService;

    @InjectMocks FestivalArtistAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/festivals/{festivalId}/artists/new ─────────────────────────

    @Test
    void 아티스트_추가_폼_조회() throws Exception {
        given(festivalService.getFestival(1L)).willReturn(mock(FestivalResponseDto.class));
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());
        given(artistFestivalService.getArtistFestivals(1L)).willReturn(List.of());

        mockMvc.perform(get("/admin/festivals/1/artists/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/festival/artist-form"))
                .andExpect(model().attributeExists("festival", "allArtists", "participatingIds", "request"));
    }

    // ── POST /admin/festivals/{festivalId}/artists ────────────────────────────

    @Test
    void 아티스트_추가_artistIds없으면_errorMessage_새_폼으로_리다이렉트() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/artists"))
                .andExpect(redirectedUrl("/admin/festivals/1/artists/new"))
                .andExpect(flash().attribute("errorMessage", "아티스트를 한 명 이상 선택해주세요."));
    }

    @Test
    void 아티스트_추가_성공_successMessage_설정() throws Exception {
        given(artistFestivalService.linkArtistsToFestival(eq(1L), any()))
                .willReturn(new ArtistFestivalService.LinkArtistsResult(1, 0, 0));

        mockMvc.perform(post("/admin/festivals/1/artists")
                        .param("artistIds", "10"))
                .andExpect(redirectedUrl("/admin/festivals/1"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void 아티스트_추가_모두_중복이면_errorMessage_설정() throws Exception {
        given(artistFestivalService.linkArtistsToFestival(eq(1L), any()))
                .willReturn(new ArtistFestivalService.LinkArtistsResult(0, 1, 0));

        mockMvc.perform(post("/admin/festivals/1/artists")
                        .param("artistIds", "10"))
                .andExpect(redirectedUrl("/admin/festivals/1"))
                .andExpect(flash().attribute("errorMessage", "선택한 아티스트가 이미 모두 참여 중입니다."));
    }

    @Test
    void 아티스트_추가_전부_실패시_errorMessage_설정() throws Exception {
        given(artistFestivalService.linkArtistsToFestival(eq(1L), any()))
                .willReturn(new ArtistFestivalService.LinkArtistsResult(0, 0, 1));

        mockMvc.perform(post("/admin/festivals/1/artists")
                        .param("artistIds", "10"))
                .andExpect(redirectedUrl("/admin/festivals/1"))
                .andExpect(flash().attribute("errorMessage", "아티스트 추가에 실패했습니다. 다시 시도해주세요."));

        then(adminLogService).should(never()).log(any(), any(), any(), any());
    }

    @Test
    void 아티스트_추가_일부_중복_일부_실패_메시지에_모두_포함() throws Exception {
        given(artistFestivalService.linkArtistsToFestival(eq(1L), any()))
                .willReturn(new ArtistFestivalService.LinkArtistsResult(1, 1, 1));

        mockMvc.perform(post("/admin/festivals/1/artists")
                        .param("artistIds", "10", "11", "12"))
                .andExpect(redirectedUrl("/admin/festivals/1"))
                .andExpect(flash().attribute("successMessage", "1명의 아티스트가 추가되었습니다. (1명은 이미 참여 중) (1명 추가 실패)"));
    }

    // ── GET /admin/festivals/{festivalId}/artists/list ────────────────────────

    @Test
    void 아티스트_목록_JSON_반환() throws Exception {
        ArtistFestivalResponseDto resp = mock(ArtistFestivalResponseDto.class);
        given(artistFestivalService.getArtistFestivals(1L)).willReturn(List.of(resp));

        mockMvc.perform(get("/admin/festivals/1/artists/list"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // ── POST /admin/festivals/{festivalId}/artists/{artistFestivalId}/edit ─────

    @Test
    void 라인업_수정_성공() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/artists/2/edit")
                        .param("stageName", "MAIN")
                        .param("performanceDate", "2026-06-01"))
                .andExpect(redirectedUrl("/admin/festivals/1#artists"))
                .andExpect(flash().attribute("successMessage", "라인업이 수정되었습니다."));
    }

    @Test
    void 라인업_수정_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류"))
                .given(artistFestivalService).updateArtistFestival(anyLong(), anyLong(), any());

        mockMvc.perform(post("/admin/festivals/1/artists/2/edit"))
                .andExpect(flash().attribute("errorMessage", "라인업 수정 중 오류가 발생했습니다."));
    }

    // ── POST /admin/festivals/{festivalId}/artists/batch-edit ─────────────────

    @Test
    void 라인업_일괄수정_성공() throws Exception {
        given(artistFestivalService.updateArtistFestivalsBatch(eq(1L), any()))
                .willReturn(new ArtistFestivalService.BatchUpdateResult(2, 0));

        mockMvc.perform(post("/admin/festivals/1/artists/batch-edit")
                        .param("afIds", "2", "3")
                        .param("performanceDates", "2026-06-01", "2026-06-02")
                        .param("stageNames", "MAIN", "SIDE"))
                .andExpect(redirectedUrl("/admin/festivals/1#artists"))
                .andExpect(flash().attribute("successMessage", "라인업이 일괄 수정되었습니다."));
    }

    @Test
    void 라인업_일괄수정_일부_실패_errorCount_포함() throws Exception {
        given(artistFestivalService.updateArtistFestivalsBatch(eq(1L), any()))
                .willReturn(new ArtistFestivalService.BatchUpdateResult(0, 1));

        mockMvc.perform(post("/admin/festivals/1/artists/batch-edit")
                        .param("afIds", "2")
                        .param("performanceDates", "2026-06-01")
                        .param("stageNames", "MAIN"))
                .andExpect(redirectedUrl("/admin/festivals/1#artists"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void 라인업_일괄수정_일부_성공_일부_실패_혼합_메시지() throws Exception {
        given(artistFestivalService.updateArtistFestivalsBatch(eq(1L), any()))
                .willReturn(new ArtistFestivalService.BatchUpdateResult(1, 1));

        mockMvc.perform(post("/admin/festivals/1/artists/batch-edit")
                        .param("afIds", "2", "3")
                        .param("performanceDates", "2026-06-01", "2026-06-02")
                        .param("stageNames", "MAIN", "SIDE"))
                .andExpect(redirectedUrl("/admin/festivals/1#artists"))
                .andExpect(flash().attribute("errorMessage", "1건 수정 완료, 1건 실패. 항목을 확인해 주세요."));

        then(adminLogService).should().log(any(), any(), any(), eq("1건 일괄 수정"));
    }

    @Test
    void 라인업_일괄수정_같은_afId_중복행은_마지막_값으로_병합() throws Exception {
        given(artistFestivalService.updateArtistFestivalsBatch(eq(1L), any()))
                .willReturn(new ArtistFestivalService.BatchUpdateResult(1, 0));

        mockMvc.perform(post("/admin/festivals/1/artists/batch-edit")
                        .param("afIds", "2", "2")
                        .param("performanceDates", "2026-06-01", "2026-06-02")
                        .param("stageNames", "MAIN", "SIDE"))
                .andExpect(redirectedUrl("/admin/festivals/1#artists"))
                .andExpect(flash().attribute("successMessage", "라인업이 일괄 수정되었습니다."));

        org.mockito.ArgumentCaptor<java.util.Map<Long, com.feple.feple_backend.artistfestival.entity.LineupUpdate>> captor =
                org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        then(artistFestivalService).should().updateArtistFestivalsBatch(eq(1L), captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(2L).stageName()).isEqualTo("SIDE");
    }

    @Test
    void 라인업_일괄수정_행개수_불일치시_요청_거부() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/artists/batch-edit")
                        .param("afIds", "2", "3")
                        .param("performanceDates", "2026-06-01")
                        .param("stageNames", "MAIN", "SIDE"))
                .andExpect(redirectedUrl("/admin/festivals/1#artists"))
                .andExpect(flash().attribute("errorMessage", "행 개수가 일치하지 않습니다. 새로고침 후 다시 시도해 주세요."));

        then(artistFestivalService).shouldHaveNoInteractions();
    }

    // ── POST /admin/festivals/{festivalId}/artists/{artistFestivalId}/delete ───

    @Test
    void 아티스트_제거_성공_successMessage_없음() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/artists/2/delete"))
                .andExpect(redirectedUrl("/admin/festivals/1#artists"))
                .andExpect(flash().attributeCount(0));

        then(artistFestivalService).should().removeArtistFromFestival(1L, 2L);
    }

    @Test
    void 아티스트_제거_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류"))
                .given(artistFestivalService).removeArtistFromFestival(anyLong(), anyLong());

        mockMvc.perform(post("/admin/festivals/1/artists/2/delete"))
                .andExpect(flash().attribute("errorMessage", "아티스트 제거 중 오류가 발생했습니다."));
    }
}
