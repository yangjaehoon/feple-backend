package com.feple.feple_backend.admin.festival;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.feple.feple_backend.artist.song.service.SetlistAdminService;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.booth.service.BoothService;
import com.feple.feple_backend.certification.service.FestivalReviewService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.stage.service.StageService;
import com.feple.feple_backend.ticketlink.service.FestivalTicketLinkService;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponseDto;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.service.TimetableService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalDetailAggregationServiceTest {

    @Mock FestivalAdminService festivalService;
    @Mock ArtistFestivalService artistFestivalService;
    @Mock TimetableService timetableService;
    @Mock StageService stageService;
    @Mock BoothService boothService;
    @Mock FestivalTicketLinkService ticketLinkService;
    @Mock SetlistAdminService setlistAdminService;
    @Mock FestivalReviewService reviewService;

    @InjectMocks FestivalDetailAggregationService aggregationService;

    private TimetableEntryResponseDto entry(String artistName, String stageName, String date) {
        return TimetableEntryResponseDto.builder()
                .id(1L).artistName(artistName).stageName(stageName).festivalDate(date)
                .build();
    }

    private ArtistFestivalResponseDto artistFestival(Long id, String name) {
        return ArtistFestivalResponseDto.builder()
                .artistFestivalId(id).artistName(name).build();
    }

    @Test
    void 평점없으면_빈_통계_반환() {
        given(festivalService.getFestival(1L)).willReturn(FestivalResponseDto.builder().id(1L).title("락페").build());
        given(timetableService.getEntries(1L)).willReturn(List.of());
        given(artistFestivalService.getArtistFestivalsWithStageFallback(eq(1L), anyMap(), anyMap())).willReturn(List.of());
        given(reviewService.getRatingCount(1L)).willReturn(0);
        given(stageService.getStages(1L)).willReturn(List.of());
        given(boothService.getBooths(1L)).willReturn(List.of());
        given(ticketLinkService.getTicketLinks(1L)).willReturn(List.of());

        FestivalDetailDto result = aggregationService.getDetail(1L);

        assertThat(result.ratingStats()).isEqualTo(FestivalRatingStatsDto.EMPTY);
        assertThat(result.setlistCounts()).isEmpty();
    }

    @Test
    void 평점있으면_통계_상세_포함() {
        given(festivalService.getFestival(1L)).willReturn(FestivalResponseDto.builder().id(1L).title("락페").build());
        given(timetableService.getEntries(1L)).willReturn(List.of());
        given(artistFestivalService.getArtistFestivalsWithStageFallback(eq(1L), anyMap(), anyMap())).willReturn(List.of());
        given(reviewService.getAverageRating(1L)).willReturn(4.5);
        given(reviewService.getRatingCount(1L)).willReturn(10);
        given(reviewService.getRatingDistribution(1L)).willReturn(Map.of(5, 8L, 4, 2L));
        given(stageService.getStages(1L)).willReturn(List.of());
        given(boothService.getBooths(1L)).willReturn(List.of());
        given(ticketLinkService.getTicketLinks(1L)).willReturn(List.of());

        FestivalDetailDto result = aggregationService.getDetail(1L);

        assertThat(result.ratingStats().averageRating()).isEqualTo(4.5);
        assertThat(result.ratingStats().ratingCount()).isEqualTo(10);
        assertThat(result.ratingStats().distribution()).containsEntry(5, 8L);
    }

    @Test
    void 타임테이블_기반_아티스트별_날짜_스테이지_매핑후_전달() {
        TimetableEntryResponseDto e1 = entry("아이유", "메인스테이지", "2026-08-01");
        TimetableEntryResponseDto e2 = entry("아이유", "메인스테이지", "2026-08-02");
        given(festivalService.getFestival(1L)).willReturn(FestivalResponseDto.builder().id(1L).title("락페").build());
        given(timetableService.getEntries(1L)).willReturn(List.of(e1, e2));
        given(artistFestivalService.getArtistFestivalsWithStageFallback(eq(1L), anyMap(), anyMap())).willReturn(List.of());
        given(reviewService.getRatingCount(1L)).willReturn(0);
        given(stageService.getStages(1L)).willReturn(List.of());
        given(boothService.getBooths(1L)).willReturn(List.of());
        given(ticketLinkService.getTicketLinks(1L)).willReturn(List.of());

        aggregationService.getDetail(1L);

        var datesCaptor = org.mockito.ArgumentCaptor.forClass(Map.class);
        var stageCaptor = org.mockito.ArgumentCaptor.forClass(Map.class);
        org.mockito.Mockito.verify(artistFestivalService).getArtistFestivalsWithStageFallback(eq(1L), datesCaptor.capture(), stageCaptor.capture());
        assertThat((List<String>) datesCaptor.getValue().get("아이유")).containsExactly("2026-08-01", "2026-08-02");
        assertThat(stageCaptor.getValue().get("아이유")).isEqualTo("메인스테이지");
    }

    @Test
    void 이름없는_타임테이블_항목은_매핑에서_제외() {
        TimetableEntryResponseDto blank = entry("", "메인스테이지", "2026-08-01");
        TimetableEntryResponseDto nullName = entry(null, "메인스테이지", "2026-08-01");
        given(festivalService.getFestival(1L)).willReturn(FestivalResponseDto.builder().id(1L).title("락페").build());
        given(timetableService.getEntries(1L)).willReturn(List.of(blank, nullName));
        given(artistFestivalService.getArtistFestivalsWithStageFallback(eq(1L), anyMap(), anyMap())).willReturn(List.of());
        given(reviewService.getRatingCount(1L)).willReturn(0);
        given(stageService.getStages(1L)).willReturn(List.of());
        given(boothService.getBooths(1L)).willReturn(List.of());
        given(ticketLinkService.getTicketLinks(1L)).willReturn(List.of());

        FestivalDetailDto result = aggregationService.getDetail(1L);

        assertThat(result.timetableByArtist()).isEmpty();
    }

    @Test
    void 공지사항_스테이지_항목은_타임테이블별_아티스트_맵에서_제외() {
        TimetableEntryResponseDto announcement = entry("공지", TimetableEntry.ANNOUNCEMENT_SENTINEL, "2026-08-01");
        given(festivalService.getFestival(1L)).willReturn(FestivalResponseDto.builder().id(1L).title("락페").build());
        given(timetableService.getEntries(1L)).willReturn(List.of(announcement));
        given(artistFestivalService.getArtistFestivalsWithStageFallback(eq(1L), anyMap(), anyMap())).willReturn(List.of());
        given(reviewService.getRatingCount(1L)).willReturn(0);
        given(stageService.getStages(1L)).willReturn(List.of());
        given(boothService.getBooths(1L)).willReturn(List.of());
        given(ticketLinkService.getTicketLinks(1L)).willReturn(List.of());

        FestivalDetailDto result = aggregationService.getDetail(1L);

        assertThat(result.timetableByArtist()).doesNotContainKey("공지");
    }

    @Test
    void 아티스트는_이름_대소문자_무시하고_정렬됨() {
        ArtistFestivalResponseDto b = artistFestival(1L, "banana");
        ArtistFestivalResponseDto A = artistFestival(2L, "Apple");
        given(festivalService.getFestival(1L)).willReturn(FestivalResponseDto.builder().id(1L).title("락페").build());
        given(timetableService.getEntries(1L)).willReturn(List.of());
        given(artistFestivalService.getArtistFestivalsWithStageFallback(eq(1L), anyMap(), anyMap())).willReturn(List.of(b, A));
        given(reviewService.getRatingCount(1L)).willReturn(0);
        given(stageService.getStages(1L)).willReturn(List.of());
        given(boothService.getBooths(1L)).willReturn(List.of());
        given(ticketLinkService.getTicketLinks(1L)).willReturn(List.of());

        FestivalDetailDto result = aggregationService.getDetail(1L);

        assertThat(result.participatingArtistsByName())
                .extracting(ArtistFestivalResponseDto::getArtistName)
                .containsExactly("Apple", "banana");
    }

    @Test
    void 참여아티스트_있으면_셋리스트_카운트_조회() {
        ArtistFestivalResponseDto artist = artistFestival(1L, "아이유");
        given(festivalService.getFestival(1L)).willReturn(FestivalResponseDto.builder().id(1L).title("락페").build());
        given(timetableService.getEntries(1L)).willReturn(List.of());
        given(artistFestivalService.getArtistFestivalsWithStageFallback(eq(1L), anyMap(), anyMap())).willReturn(List.of(artist));
        given(reviewService.getRatingCount(1L)).willReturn(0);
        given(stageService.getStages(1L)).willReturn(List.of());
        given(boothService.getBooths(1L)).willReturn(List.of());
        given(ticketLinkService.getTicketLinks(1L)).willReturn(List.of());
        given(setlistAdminService.getSetlistCounts(List.of(1L))).willReturn(Map.of(1L, 5));

        FestivalDetailDto result = aggregationService.getDetail(1L);

        assertThat(result.setlistCounts()).containsEntry(1L, 5);
    }

    @Test
    void 참여아티스트_없으면_셋리스트_조회_스킵() {
        given(festivalService.getFestival(1L)).willReturn(FestivalResponseDto.builder().id(1L).title("락페").build());
        given(timetableService.getEntries(1L)).willReturn(List.of());
        given(artistFestivalService.getArtistFestivalsWithStageFallback(eq(1L), anyMap(), anyMap())).willReturn(List.of());
        given(reviewService.getRatingCount(1L)).willReturn(0);
        given(stageService.getStages(1L)).willReturn(List.of());
        given(boothService.getBooths(1L)).willReturn(List.of());
        given(ticketLinkService.getTicketLinks(1L)).willReturn(List.of());

        FestivalDetailDto result = aggregationService.getDetail(1L);

        assertThat(result.setlistCounts()).isEmpty();
        org.mockito.Mockito.verify(setlistAdminService, org.mockito.Mockito.never()).getSetlistCounts(any());
    }

    @Test
    void 구글맵키_없으면_경고로그만_남기고_정상_동작() {
        ReflectionTestUtils.setField(aggregationService, "googleMapsKey", "");
        given(festivalService.getFestival(1L)).willReturn(FestivalResponseDto.builder().id(1L).title("락페").build());
        given(timetableService.getEntries(1L)).willReturn(List.of());
        given(artistFestivalService.getArtistFestivalsWithStageFallback(eq(1L), anyMap(), anyMap())).willReturn(List.of());
        given(reviewService.getRatingCount(1L)).willReturn(0);
        given(stageService.getStages(1L)).willReturn(List.of());
        given(boothService.getBooths(1L)).willReturn(List.of());
        given(ticketLinkService.getTicketLinks(1L)).willReturn(List.of());

        FestivalDetailDto result = aggregationService.getDetail(1L);

        assertThat(result.googleMapsKey()).isEmpty();
    }
}
