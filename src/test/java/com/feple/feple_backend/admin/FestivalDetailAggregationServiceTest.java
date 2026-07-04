package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.festival.FestivalDetailAggregationService;
import com.feple.feple_backend.admin.festival.FestivalDetailDto;
import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.booth.service.BoothService;
import com.feple.feple_backend.certification.service.FestivalReviewService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.stage.service.StageService;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponseDto;
import com.feple.feple_backend.timetable.service.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FestivalDetailAggregationServiceTest {

    @Mock FestivalService festivalService;
    @Mock ArtistFestivalService artistFestivalService;
    @Mock TimetableService timetableService;
    @Mock StageService stageService;
    @Mock BoothService boothService;
    @Mock SongAdminService songAdminService;
    @Mock FestivalReviewService reviewService;

    @InjectMocks FestivalDetailAggregationService service;

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    /** 아티스트 목록이 비어있을 때 — songAdminService 호출 없음 */
    private void stubBase(Long festivalId, List<TimetableEntryResponseDto> entries,
                          List<ArtistFestivalResponseDto> artists) {
        given(festivalService.getFestival(festivalId)).willReturn(mock(FestivalResponseDto.class));
        given(timetableService.getEntries(festivalId)).willReturn(entries);
        given(artistFestivalService.getArtistFestivals(eq(festivalId), any(), any())).willReturn(artists);
        given(stageService.getStages(festivalId)).willReturn(List.of());
        given(boothService.getBooths(festivalId)).willReturn(List.of());
        given(reviewService.getAverageRating(festivalId)).willReturn(0.0);
        given(reviewService.getRatingCount(festivalId)).willReturn(0);
    }

    /** 아티스트가 있을 때 — songAdminService 스텁 포함 */
    private void stubBaseWithArtists(Long festivalId, List<TimetableEntryResponseDto> entries,
                                     List<ArtistFestivalResponseDto> artists) {
        stubBase(festivalId, entries, artists);
        given(songAdminService.getSetlistCounts(any())).willReturn(Map.of());
    }

    private static TimetableEntryResponseDto entry(String artistName, String stageName, String date) {
        return TimetableEntryResponseDto.builder()
                .artistName(artistName)
                .stageName(stageName)
                .festivalDate(date)
                .build();
    }

    private static ArtistFestivalResponseDto artist(Long afId, String name) {
        return ArtistFestivalResponseDto.builder()
                .artistFestivalId(afId)
                .artistName(name)
                .build();
    }

    // ── 서비스 위임 검증 ──────────────────────────────────────────────────────

    @Test
    void getDetail_모든_서비스에_festivalId_전달() {
        Long festivalId = 1L;
        // 아티스트 없음 → songAdminService 미호출
        stubBase(festivalId, List.of(), List.of());

        service.getDetail(festivalId);

        verify(festivalService).getFestival(festivalId);
        verify(timetableService).getEntries(festivalId);
        verify(stageService).getStages(festivalId);
        verify(boothService).getBooths(festivalId);
    }

    // ── buildDatesByArtistName ────────────────────────────────────────────────

    @Test
    void 아티스트명_null_항목은_날짜맵에서_제외() {
        Long festivalId = 1L;
        List<TimetableEntryResponseDto> entries = List.of(
                entry(null,      "Main", "2025-06-22"),
                entry("Artist1", "Main", "2025-06-22")
        );
        stubBase(festivalId, entries, List.of());

        service.getDetail(festivalId);

        ArgumentCaptor<Map<String, List<String>>> datesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(artistFestivalService).getArtistFestivals(eq(festivalId), datesCaptor.capture(), any());
        assertThat(datesCaptor.getValue())
                .containsKey("Artist1")
                .doesNotContainKey(null);
    }

    @Test
    void 같은_아티스트의_여러_날짜가_모두_수집됨() {
        Long festivalId = 1L;
        List<TimetableEntryResponseDto> entries = List.of(
                entry("Artist1", "Main", "2025-06-21"),
                entry("Artist1", "Main", "2025-06-22")
        );
        stubBase(festivalId, entries, List.of());

        service.getDetail(festivalId);

        ArgumentCaptor<Map<String, List<String>>> datesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(artistFestivalService).getArtistFestivals(eq(festivalId), datesCaptor.capture(), any());
        assertThat(datesCaptor.getValue().get("Artist1"))
                .containsExactlyInAnyOrder("2025-06-21", "2025-06-22");
    }

    // ── buildStageByArtistName ────────────────────────────────────────────────

    @Test
    void 스테이지명_null_항목은_스테이지맵에서_제외() {
        Long festivalId = 1L;
        List<TimetableEntryResponseDto> entries = List.of(
                entry("Artist1", null,  "2025-06-22"),
                entry("Artist2", "Sub", "2025-06-22")
        );
        stubBase(festivalId, entries, List.of());

        service.getDetail(festivalId);

        ArgumentCaptor<Map<String, String>> stageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(artistFestivalService).getArtistFestivals(eq(festivalId), any(), stageCaptor.capture());
        assertThat(stageCaptor.getValue())
                .containsEntry("Artist2", "Sub")
                .doesNotContainKey("Artist1");
    }

    @Test
    void 중복_아티스트의_스테이지는_첫번째_항목이_유지됨() {
        Long festivalId = 1L;
        List<TimetableEntryResponseDto> entries = List.of(
                entry("Artist1", "Main", "2025-06-21"),
                entry("Artist1", "Sub",  "2025-06-22")
        );
        stubBase(festivalId, entries, List.of());

        service.getDetail(festivalId);

        ArgumentCaptor<Map<String, String>> stageCaptor = ArgumentCaptor.forClass(Map.class);
        verify(artistFestivalService).getArtistFestivals(eq(festivalId), any(), stageCaptor.capture());
        assertThat(stageCaptor.getValue().get("Artist1")).isEqualTo("Main");
    }

    // ── buildTimetableByArtist ────────────────────────────────────────────────

    @Test
    void ANNOUNCEMENT_STAGE_항목은_타임테이블맵에서_제외() {
        Long festivalId = 1L;
        ArtistFestivalResponseDto artist1 = artist(10L, "Artist1");
        // "📢" 스테이지 항목만 있음 → 필터되어 Artist1 에 빈 목록 추가됨
        List<TimetableEntryResponseDto> entries = List.of(
                entry("Artist1", "📢", "2025-06-22")
        );
        stubBaseWithArtists(festivalId, entries, List.of(artist1));

        FestivalDetailDto model = service.getDetail(festivalId);

        assertThat(model.timetableByArtist().get("Artist1")).isEmpty();
    }

    @Test
    void 타임테이블_없는_아티스트에게_빈목록_추가() {
        Long festivalId = 1L;
        ArtistFestivalResponseDto artist1 = artist(10L, "Artist1");
        stubBaseWithArtists(festivalId, List.of(), List.of(artist1));

        FestivalDetailDto model = service.getDetail(festivalId);

        assertThat(model.timetableByArtist()).containsKey("Artist1");
        assertThat(model.timetableByArtist().get("Artist1")).isEmpty();
    }

    @Test
    void 아티스트의_타임테이블_항목이_맵에_포함됨() {
        Long festivalId = 1L;
        TimetableEntryResponseDto e = entry("Artist1", "Main", "2025-06-22");
        ArtistFestivalResponseDto artist1 = artist(10L, "Artist1");
        stubBaseWithArtists(festivalId, List.of(e), List.of(artist1));

        FestivalDetailDto model = service.getDetail(festivalId);

        assertThat(model.timetableByArtist().get("Artist1")).containsExactly(e);
    }

    // ── sortArtistsByName ─────────────────────────────────────────────────────

    @Test
    void 아티스트_이름_기준_대소문자_무관_정렬() {
        Long festivalId = 1L;
        List<ArtistFestivalResponseDto> artists = List.of(
                artist(3L, "zeppelin"),
                artist(1L, "Arctic Monkeys"),
                artist(2L, "blur")
        );
        stubBaseWithArtists(festivalId, List.of(), artists);

        FestivalDetailDto model = service.getDetail(festivalId);

        assertThat(model.participatingArtistsByName())
                .extracting(ArtistFestivalResponseDto::getArtistName)
                .containsExactly("Arctic Monkeys", "blur", "zeppelin");
    }

    // ── buildSetlistCounts ────────────────────────────────────────────────────

    @Test
    void 아티스트_없으면_셋리스트_조회_안함() {
        Long festivalId = 1L;
        stubBase(festivalId, List.of(), List.of());

        service.getDetail(festivalId);

        verify(songAdminService, never()).getSetlistCounts(any());
    }

    @Test
    void 아티스트_있으면_artistFestivalId_목록으로_셋리스트_조회() {
        Long festivalId = 1L;
        List<ArtistFestivalResponseDto> artists = List.of(artist(10L, "A"), artist(20L, "B"));
        stubBase(festivalId, List.of(), artists);
        given(songAdminService.getSetlistCounts(List.of(10L, 20L))).willReturn(Map.of(10L, 3, 20L, 1));

        service.getDetail(festivalId);

        verify(songAdminService).getSetlistCounts(List.of(10L, 20L));
    }
}
