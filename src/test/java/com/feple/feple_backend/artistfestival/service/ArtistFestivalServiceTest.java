package com.feple.feple_backend.artistfestival.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequestDto;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.dto.ArtistNameOption;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.event.ArtistAddedToFestivalEvent;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.repository.StageRepository;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ArtistFestivalServiceTest {

    @Mock ArtistFestivalRepository artistFestivalRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock ArtistRepository artistRepository;
    @Mock FileStorageService fileStorageService;
    @Mock TimetableRepository timetableRepository;
    @Mock StageRepository stageRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks ArtistFestivalService service;

    private Artist artist(Long id, String name) {
        return Artist.builder().id(id).name(name).nameEn(name + "_EN").build();
    }

    private Festival festival(Long id, LocalDate startDate) {
        return Festival.builder().id(id).title("펜타포트").titleEn("Pentaport").startDate(startDate).build();
    }

    private ArtistFestival artistFestival(Artist artist, Festival festival) {
        return ArtistFestival.builder().artist(artist).festival(festival).build();
    }

    // ── getArtistFestivals ──────────────────────────────────────────────────

    @Test
    void 아티스트_목록_조회_시_타임테이블에서_공연일자_매핑() {
        Artist artist = artist(1L, "아이유");
        ArtistFestival af = artistFestival(artist, festival(100L, null));
        given(artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(100L)).willReturn(List.of(af));

        TimetableEntry entry = TimetableEntry.builder()
                .artistName("아이유").festivalDate(LocalDate.of(2026, 8, 1)).build();
        given(timetableRepository.findByFestivalIdWithStage(100L)).willReturn(List.of(entry));

        List<ArtistFestivalResponseDto> result = service.getArtistFestivals(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPerformanceDate()).isEqualTo("2026-08-01");
    }

    @Test
    void 아티스트_목록_조회_사전로드된_날짜맵_재사용시_타임테이블_재조회_안함() {
        Artist artist = artist(1L, "아이유");
        ArtistFestival af = artistFestival(artist, festival(100L, null));
        given(artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(100L)).willReturn(List.of(af));

        List<ArtistFestivalResponseDto> result =
                service.getArtistFestivals(100L, Map.of("아이유", List.of("2026-08-02")));

        assertThat(result.get(0).getPerformanceDate()).isEqualTo("2026-08-02");
        then(timetableRepository).should(never()).findByFestivalIdWithStage(any());
    }

    @Test
    void 아티스트_목록_조회_관리자용은_스테이지_폴백_적용() {
        Artist artist = artist(1L, "아이유");
        ArtistFestival af = artistFestival(artist, festival(100L, null));
        given(artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(100L)).willReturn(List.of(af));

        List<ArtistFestivalResponseDto> result = service.getArtistFestivals(
                100L, Map.of(), Map.of("아이유", "메인스테이지"));

        assertThat(result.get(0).getStageName()).isEqualTo("메인스테이지");
    }

    // ── addArtistToFestival ───────────────────────────────────────────────

    @Test
    void 아티스트_추가_성공() {
        Festival festival = festival(100L, LocalDate.now().plusDays(10));
        Artist artist = artist(1L, "아이유");
        given(festivalRepository.findById(100L)).willReturn(Optional.of(festival));
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(artistFestivalRepository.existsByFestivalIdAndArtistId(100L, 1L)).willReturn(false);
        ArtistFestival saved = mock(ArtistFestival.class);
        given(saved.getId()).willReturn(999L);
        given(artistFestivalRepository.save(any())).willReturn(saved);

        ArtistFestivalCreateRequestDto req = new ArtistFestivalCreateRequestDto();
        req.setArtistId(1L);

        Long resultId = service.addArtistToFestival(100L, req);

        assertThat(resultId).isEqualTo(999L);
    }

    @Test
    void 아티스트_추가_이미_참여중이면_예외() {
        given(festivalRepository.findById(100L)).willReturn(Optional.of(festival(100L, null)));
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist(1L, "아이유")));
        given(artistFestivalRepository.existsByFestivalIdAndArtistId(100L, 1L)).willReturn(true);

        ArtistFestivalCreateRequestDto req = new ArtistFestivalCreateRequestDto();
        req.setArtistId(1L);

        assertThatThrownBy(() -> service.addArtistToFestival(100L, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 이 페스티벌에 참여 중인 아티스트입니다.");
    }

    @Test
    void 아티스트_추가_페스티벌_없으면_예외() {
        given(festivalRepository.findById(100L)).willReturn(Optional.empty());

        ArtistFestivalCreateRequestDto req = new ArtistFestivalCreateRequestDto();
        req.setArtistId(1L);

        assertThatThrownBy(() -> service.addArtistToFestival(100L, req))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 아티스트_추가_아티스트_없으면_예외() {
        given(festivalRepository.findById(100L)).willReturn(Optional.of(festival(100L, null)));
        given(artistRepository.findById(1L)).willReturn(Optional.empty());

        ArtistFestivalCreateRequestDto req = new ArtistFestivalCreateRequestDto();
        req.setArtistId(1L);

        assertThatThrownBy(() -> service.addArtistToFestival(100L, req))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 아티스트_추가_시작전_페스티벌이면_이벤트_발행() {
        Festival festival = festival(100L, LocalDate.now().plusDays(1));
        Artist artist = artist(1L, "아이유");
        given(festivalRepository.findById(100L)).willReturn(Optional.of(festival));
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(artistFestivalRepository.existsByFestivalIdAndArtistId(100L, 1L)).willReturn(false);
        given(artistFestivalRepository.save(any())).willReturn(artistFestival(artist, festival));

        ArtistFestivalCreateRequestDto req = new ArtistFestivalCreateRequestDto();
        req.setArtistId(1L);

        service.addArtistToFestival(100L, req);

        then(eventPublisher).should().publishEvent(any(ArtistAddedToFestivalEvent.class));
    }

    @Test
    void 아티스트_추가_이미_시작한_페스티벌이면_이벤트_미발행() {
        Festival festival = festival(100L, LocalDate.now().minusDays(1));
        Artist artist = artist(1L, "아이유");
        given(festivalRepository.findById(100L)).willReturn(Optional.of(festival));
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(artistFestivalRepository.existsByFestivalIdAndArtistId(100L, 1L)).willReturn(false);
        given(artistFestivalRepository.save(any())).willReturn(artistFestival(artist, festival));

        ArtistFestivalCreateRequestDto req = new ArtistFestivalCreateRequestDto();
        req.setArtistId(1L);

        service.addArtistToFestival(100L, req);

        then(eventPublisher).should(never()).publishEvent(any());
    }

    // ── linkArtistsToFestival ─────────────────────────────────────────────

    @Test
    void 아티스트_일괄연결_빈리스트면_아무일도_안함() {
        service.linkArtistsToFestival(100L, List.of());

        then(festivalRepository).shouldHaveNoInteractions();
    }

    @Test
    void 아티스트_일괄연결_null이면_아무일도_안함() {
        service.linkArtistsToFestival(100L, null);

        then(festivalRepository).shouldHaveNoInteractions();
    }

    @Test
    void 아티스트_일괄연결_이미_참여중인_아티스트는_건너뛰고_계속() {
        Festival festival = festival(100L, null);
        given(festivalRepository.findById(100L)).willReturn(Optional.of(festival));
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist(1L, "아이유")));
        given(artistRepository.findById(2L)).willReturn(Optional.of(artist(2L, "뉴진스")));
        given(artistFestivalRepository.existsByFestivalIdAndArtistId(100L, 1L)).willReturn(true);
        given(artistFestivalRepository.existsByFestivalIdAndArtistId(100L, 2L)).willReturn(false);
        given(artistFestivalRepository.save(any())).willReturn(artistFestival(artist(2L, "뉴진스"), festival));

        service.linkArtistsToFestival(100L, List.of(1L, 2L));

        then(artistFestivalRepository).should().save(any());
    }

    @Test
    void 아티스트_일괄연결_존재하지않는_아티스트는_건너뛰고_계속() {
        Festival festival = festival(100L, null);
        given(festivalRepository.findById(100L)).willReturn(Optional.of(festival));
        given(artistRepository.findById(1L)).willReturn(Optional.empty());
        given(artistRepository.findById(2L)).willReturn(Optional.of(artist(2L, "뉴진스")));
        given(artistFestivalRepository.existsByFestivalIdAndArtistId(100L, 2L)).willReturn(false);
        given(artistFestivalRepository.save(any())).willReturn(artistFestival(artist(2L, "뉴진스"), festival));

        service.linkArtistsToFestival(100L, List.of(1L, 2L));

        then(artistFestivalRepository).should().save(any());
    }

    // ── updateArtistFestival ──────────────────────────────────────────────

    @Test
    void 참여정보_수정_다른_페스티벌이면_예외() {
        ArtistFestival af = artistFestival(artist(1L, "아이유"), festival(200L, null));
        given(artistFestivalRepository.findById(10L)).willReturn(Optional.of(af));

        assertThatThrownBy(() -> service.updateArtistFestival(100L, 10L, "메인스테이지", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 페스티벌입니다.");
    }

    @Test
    void 참여정보_수정_스테이지_변경시_타임테이블도_동기화() {
        Festival festival = festival(100L, null);
        Artist artist = artist(1L, "아이유");
        ArtistFestival af = ArtistFestival.builder().artist(artist).festival(festival).stageName("서브스테이지").build();
        given(artistFestivalRepository.findById(10L)).willReturn(Optional.of(af));

        Stage newStage = mock(Stage.class);
        given(newStage.getName()).willReturn("메인스테이지");
        given(stageRepository.findByFestivalIdAndName(100L, "메인스테이지")).willReturn(Optional.of(newStage));

        TimetableEntry entry = TimetableEntry.builder()
                .artistName("아이유").festivalDate(LocalDate.of(2026, 8, 1)).build();
        given(timetableRepository.findByFestivalIdAndArtistName(100L, "아이유")).willReturn(List.of(entry));

        service.updateArtistFestival(100L, 10L, "메인스테이지", null);

        assertThat(af.getStageName()).isEqualTo("메인스테이지");
        assertThat(entry.getStageName()).isEqualTo("메인스테이지");
    }

    @Test
    void 참여정보_수정_스테이지_없으면_타임테이블_동기화_안함() {
        Festival festival = festival(100L, null);
        Artist artist = artist(1L, "아이유");
        ArtistFestival af = ArtistFestival.builder().artist(artist).festival(festival).build();
        given(artistFestivalRepository.findById(10L)).willReturn(Optional.of(af));

        service.updateArtistFestival(100L, 10L, "", null);

        assertThat(af.getStageName()).isNull();
        then(timetableRepository).shouldHaveNoInteractions();
    }

    @Test
    void 참여정보_수정_날짜_변경시_기존날짜_있으면_해당날짜_항목만_동기화() {
        Festival festival = festival(100L, null);
        Artist artist = artist(1L, "아이유");
        ArtistFestival af = ArtistFestival.builder().artist(artist).festival(festival)
                .performanceDate(LocalDate.of(2026, 8, 1)).build();
        given(artistFestivalRepository.findById(10L)).willReturn(Optional.of(af));

        TimetableEntry sameDate = TimetableEntry.builder()
                .artistName("아이유").festivalDate(LocalDate.of(2026, 8, 1)).build();
        TimetableEntry otherDate = TimetableEntry.builder()
                .artistName("아이유").festivalDate(LocalDate.of(2026, 8, 2)).build();
        given(timetableRepository.findByFestivalIdAndArtistName(100L, "아이유"))
                .willReturn(List.of(sameDate, otherDate));

        service.updateArtistFestival(100L, 10L, null, LocalDate.of(2026, 8, 3));

        assertThat(sameDate.getFestivalDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(otherDate.getFestivalDate()).isEqualTo(LocalDate.of(2026, 8, 2));
    }

    @Test
    void 참여정보_수정_날짜_변경시_기존날짜_없으면_전체_동기화() {
        Festival festival = festival(100L, null);
        Artist artist = artist(1L, "아이유");
        ArtistFestival af = ArtistFestival.builder().artist(artist).festival(festival).build();
        given(artistFestivalRepository.findById(10L)).willReturn(Optional.of(af));

        TimetableEntry entry1 = TimetableEntry.builder().artistName("아이유").festivalDate(LocalDate.of(2026, 8, 1)).build();
        TimetableEntry entry2 = TimetableEntry.builder().artistName("아이유").festivalDate(LocalDate.of(2026, 8, 2)).build();
        given(timetableRepository.findByFestivalIdAndArtistName(100L, "아이유"))
                .willReturn(List.of(entry1, entry2));

        service.updateArtistFestival(100L, 10L, null, LocalDate.of(2026, 8, 3));

        assertThat(entry1.getFestivalDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(entry2.getFestivalDate()).isEqualTo(LocalDate.of(2026, 8, 3));
    }

    // ── syncFromTimetableEntry ────────────────────────────────────────────

    @Test
    void 타임테이블에서_동기화_아티스트명_공백이면_무시() {
        service.syncFromTimetableEntry(100L, " ", LocalDate.now(), "스테이지");

        then(artistFestivalRepository).shouldHaveNoInteractions();
    }

    @Test
    void 타임테이블에서_동기화_참여정보_있으면_업데이트() {
        ArtistFestival af = artistFestival(artist(1L, "아이유"), festival(100L, null));
        given(artistFestivalRepository.findByFestivalIdAndArtistName(100L, "아이유")).willReturn(Optional.of(af));

        service.syncFromTimetableEntry(100L, "아이유", LocalDate.of(2026, 8, 1), "메인스테이지");

        assertThat(af.getStageName()).isEqualTo("메인스테이지");
        assertThat(af.getPerformanceDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void 타임테이블에서_동기화_참여정보_없으면_무시() {
        given(artistFestivalRepository.findByFestivalIdAndArtistName(100L, "아이유")).willReturn(Optional.empty());

        service.syncFromTimetableEntry(100L, "아이유", LocalDate.now(), "메인스테이지");
        // 예외 없이 조용히 무시됨
    }

    // ── 기타 조회 위임 ─────────────────────────────────────────────────────

    @Test
    void 영문명_포함_목록_조회() {
        ArtistFestival af = artistFestival(artist(1L, "아이유"), festival(100L, null));
        given(artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(100L)).willReturn(List.of(af));

        List<ArtistNameOption> result = service.getArtistFestivalsWithEnName(100L);

        assertThat(result.get(0).name()).isEqualTo("아이유");
        assertThat(result.get(0).nameEn()).isEqualTo("아이유_EN");
    }

    @Test
    void 아티스트별_참여이력_조회() {
        List<ArtistFestival> list = List.of(artistFestival(artist(1L, "아이유"), festival(100L, null)));
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateDesc(1L)).willReturn(list);

        assertThat(service.getAppearancesByArtistId(1L)).isEqualTo(list);
    }

    @Test
    void 참여정보_ID로_조회_성공() {
        ArtistFestival af = artistFestival(artist(1L, "아이유"), festival(100L, null));
        given(artistFestivalRepository.findByIdWithFestival(10L)).willReturn(Optional.of(af));

        assertThat(service.getArtistFestivalById(10L)).isEqualTo(af);
    }

    @Test
    void 참여정보_ID로_조회_실패시_예외() {
        given(artistFestivalRepository.findByIdWithFestival(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getArtistFestivalById(10L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 아티스트_소속_여부_확인() {
        given(artistFestivalRepository.existsByIdAndArtistId(10L, 1L)).willReturn(true);

        assertThat(service.existsByIdAndArtistId(10L, 1L)).isTrue();
    }

    @Test
    void 참여정보_아티스트_불일치시_예외() {
        given(artistFestivalRepository.existsByIdAndArtistId(10L, 1L)).willReturn(false);

        assertThatThrownBy(() -> service.getArtistFestivalByIdAndArtistId(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 아티스트의 셋리스트가 아닙니다.");
    }

    @Test
    void 참여정보_아티스트_일치시_조회() {
        ArtistFestival af = artistFestival(artist(1L, "아이유"), festival(100L, null));
        given(artistFestivalRepository.existsByIdAndArtistId(10L, 1L)).willReturn(true);
        given(artistFestivalRepository.findByIdWithFestival(10L)).willReturn(Optional.of(af));

        assertThat(service.getArtistFestivalByIdAndArtistId(10L, 1L)).isEqualTo(af);
    }

    // ── removeArtistFromFestival ──────────────────────────────────────────

    @Test
    void 참여정보_삭제_성공() {
        ArtistFestival af = artistFestival(artist(1L, "아이유"), festival(100L, null));
        given(artistFestivalRepository.findById(10L)).willReturn(Optional.of(af));

        service.removeArtistFromFestival(100L, 10L);

        ArgumentCaptor<ArtistFestival> captor = ArgumentCaptor.forClass(ArtistFestival.class);
        then(artistFestivalRepository).should().delete(captor.capture());
        assertThat(captor.getValue()).isEqualTo(af);
    }

    @Test
    void 참여정보_삭제_다른_페스티벌이면_예외() {
        ArtistFestival af = artistFestival(artist(1L, "아이유"), festival(200L, null));
        given(artistFestivalRepository.findById(10L)).willReturn(Optional.of(af));

        assertThatThrownBy(() -> service.removeArtistFromFestival(100L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 페스티벌입니다.");
        then(artistFestivalRepository).should(never()).delete(any());
    }
}
