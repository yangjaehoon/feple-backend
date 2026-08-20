package com.feple.feple_backend.artistfestival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artistfestival.dto.ArtistScheduleResponseDto;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.EventType;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.file.service.FileStorageService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtistScheduleServiceTest {

    // CALLS_REAL_METHODS — ArtistFestivalRepository의 default 메서드(findByFestivalIdInGroupedByFestivalId)가
    // 실제 본문을 실행하도록 함(그래야 아래에서 findByFestivalIdInWithArtist만 스텁해도 coArtistMap이 채워짐).
    @Mock(answer = Answers.CALLS_REAL_METHODS) ArtistFestivalRepository artistFestivalRepository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks ArtistScheduleService service;

    private Artist artist(Long id, String name) {
        return Artist.builder().id(id).name(name).nameEn(name + "En").build();
    }

    private Festival festival(Long id) {
        return Festival.builder().id(id).title("페스티벌" + id).description("설명")
                .location("서울").startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 3))
                .posterKey("poster.jpg").eventType(EventType.FESTIVAL).build();
    }

    private ArtistFestival artistFestival(Artist artist, Festival festival, LocalDate performanceDate) {
        return ArtistFestival.builder().artist(artist).festival(festival).performanceDate(performanceDate).build();
    }

    @Test
    void 출연내역_없으면_빈목록_공동출연자_조회_생략() {
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of());

        List<ArtistScheduleResponseDto> result = service.getArtistSchedule(1L);

        assertThat(result).isEmpty();
        verify(artistFestivalRepository, never()).findByFestivalIdInWithArtist(any());
    }

    @Test
    void performanceDate_있으면_시작일_종료일_모두_해당날짜() {
        Artist me = artist(1L, "나");
        Festival fest = festival(10L);
        ArtistFestival af = artistFestival(me, fest, LocalDate.of(2026, 8, 2));
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of(af));
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(10L))).willReturn(List.of(af));

        List<ArtistScheduleResponseDto> result = service.getArtistSchedule(1L);

        assertThat(result.get(0).getStartDate()).isEqualTo(LocalDate.of(2026, 8, 2));
        assertThat(result.get(0).getEndDate()).isEqualTo(LocalDate.of(2026, 8, 2));
    }

    @Test
    void performanceDate_없으면_페스티벌_기간_사용() {
        Artist me = artist(1L, "나");
        Festival fest = festival(10L);
        ArtistFestival af = artistFestival(me, fest, null);
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of(af));
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(10L))).willReturn(List.of(af));

        List<ArtistScheduleResponseDto> result = service.getArtistSchedule(1L);

        assertThat(result.get(0).getStartDate()).isEqualTo(fest.getStartDate());
        assertThat(result.get(0).getEndDate()).isEqualTo(fest.getEndDate());
    }

    @Test
    void 공동출연자는_본인을_제외하고_포함() {
        Artist me = artist(1L, "나");
        Artist other = artist(2L, "동반출연자");
        Festival fest = festival(10L);
        ArtistFestival myEntry = artistFestival(me, fest, null);
        ArtistFestival otherEntry = artistFestival(other, fest, null);
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of(myEntry));
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(10L)))
                .willReturn(List.of(myEntry, otherEntry));
        given(fileStorageService.buildUrl(any())).willReturn("https://img.example.com/x.jpg");

        List<ArtistScheduleResponseDto> result = service.getArtistSchedule(1L);

        assertThat(result.get(0).getCoArtists()).hasSize(1);
        assertThat(result.get(0).getCoArtists().get(0).getArtistId()).isEqualTo(2L);
        assertThat(result.get(0).getCoArtists().get(0).getArtistName()).isEqualTo("동반출연자");
    }

    @Test
    void 단독출연이면_공동출연자_없음() {
        Artist me = artist(1L, "나");
        Festival fest = festival(10L);
        ArtistFestival myEntry = artistFestival(me, fest, null);
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of(myEntry));
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(10L))).willReturn(List.of(myEntry));

        List<ArtistScheduleResponseDto> result = service.getArtistSchedule(1L);

        assertThat(result.get(0).getCoArtists()).isEmpty();
    }
}
