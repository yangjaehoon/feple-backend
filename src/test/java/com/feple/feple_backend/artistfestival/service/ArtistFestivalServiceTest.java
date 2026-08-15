package com.feple.feple_backend.artistfestival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequestDto;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponseDto;
import com.feple.feple_backend.artistfestival.dto.ArtistNameOption;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.entity.LineupUpdate;
import com.feple.feple_backend.artistfestival.event.ArtistAddedToFestivalEvent;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import com.feple.feple_backend.timetable.service.TimetableSyncService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArtistFestivalServiceTest {

    @Mock ArtistFestivalRepository artistFestivalRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock ArtistRepository artistRepository;
    @Mock FileStorageService fileStorageService;
    @Mock TimetableRepository timetableRepository;
    @Mock TimetableSyncService timetableSyncService;
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
    void 아티스트_목록_조회_관리자용은_스테이지_폴백_적용() {
        Artist artist = artist(1L, "아이유");
        ArtistFestival af = artistFestival(artist, festival(100L, null));
        given(artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(100L)).willReturn(List.of(af));

        List<ArtistFestivalResponseDto> result = service.getArtistFestivalsWithStageFallback(
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
        // 프로덕션 코드(ArtistFestivalService)가 Asia/Seoul 기준으로 "오늘"을 계산하므로
        // 테스트도 동일 zone으로 맞춰야 CI 러너(UTC)에서 자정~오전9시(KST) 사이 실행 시
        // 날짜 경계에서 어긋나지 않는다
        Festival festival = festival(100L, LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1));
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
        Festival festival = festival(100L, LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1));
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
        Artist existingArtist = artist(1L, "아이유");
        given(festivalRepository.findById(100L)).willReturn(Optional.of(festival));
        given(artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(100L))
                .willReturn(List.of(artistFestival(existingArtist, festival)));
        given(artistRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(existingArtist, artist(2L, "뉴진스")));

        ArtistFestivalService.LinkArtistsResult result = service.linkArtistsToFestival(100L, List.of(1L, 2L));

        assertThat(result.added()).isEqualTo(1);
        assertThat(result.duplicates()).isEqualTo(1);
        assertThat(result.errors()).isEqualTo(0);
        ArgumentCaptor<List<ArtistFestival>> captor = ArgumentCaptor.forClass(List.class);
        then(artistFestivalRepository).should().saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getArtistId()).isEqualTo(2L);
    }

    @Test
    void 아티스트_일괄연결_존재하지않는_아티스트는_건너뛰고_계속() {
        Festival festival = festival(100L, null);
        given(festivalRepository.findById(100L)).willReturn(Optional.of(festival));
        given(artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(100L)).willReturn(List.of());
        // artistId=1은 findAllById 결과에 없음 → 존재하지 않는 아티스트로 취급
        given(artistRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(artist(2L, "뉴진스")));

        ArtistFestivalService.LinkArtistsResult result = service.linkArtistsToFestival(100L, List.of(1L, 2L));

        assertThat(result.added()).isEqualTo(1);
        assertThat(result.errors()).isEqualTo(1);
        ArgumentCaptor<List<ArtistFestival>> captor = ArgumentCaptor.forClass(List.class);
        then(artistFestivalRepository).should().saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    void 아티스트_일괄연결_날짜맵에_있는_아티스트만_performanceDate_설정() {
        // 라인업 OCR처럼 아티스트별 출연일을 함께 등록하는 3-파라미터 오버로드 —
        // 맵에 없는 아티스트는 날짜 없이 등록되고(이후 타임테이블 등록 시 보충), 있는 아티스트만 반영돼야 한다
        Festival festival = festival(100L, null);
        given(festivalRepository.findById(100L)).willReturn(Optional.of(festival));
        given(artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(100L)).willReturn(List.of());
        given(artistRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(artist(1L, "아이유"), artist(2L, "뉴진스")));

        service.linkArtistsToFestival(100L, List.of(1L, 2L), Map.of(1L, LocalDate.of(2026, 8, 1)));

        ArgumentCaptor<List<ArtistFestival>> captor = ArgumentCaptor.forClass(List.class);
        then(artistFestivalRepository).should().saveAll(captor.capture());
        List<ArtistFestival> saved = captor.getValue();
        assertThat(saved.stream().filter(af -> af.getArtistId().equals(1L)).findFirst().orElseThrow().getPerformanceDate())
                .isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(saved.stream().filter(af -> af.getArtistId().equals(2L)).findFirst().orElseThrow().getPerformanceDate())
                .isNull();
    }

    // ── updateArtistFestival ──────────────────────────────────────────────

    @Test
    void 참여정보_수정_다른_페스티벌이면_예외() {
        ArtistFestival af = artistFestival(artist(1L, "아이유"), festival(200L, null));
        given(artistFestivalRepository.findById(10L)).willReturn(Optional.of(af));

        assertThatThrownBy(() -> service.updateArtistFestival(100L, 10L, new LineupUpdate("메인스테이지", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 페스티벌입니다.");
    }

    // 스테이지/날짜 값에 따른 분기(변경 없음/기존값 있음/없음)는 TimetableSyncServiceTest에서 검증한다.
    // 여기서는 ArtistFestivalService가 TimetableSyncService에 올바른 인자로 위임하는지만 확인한다.
    @Test
    void 참여정보_수정_스테이지_변경시_타임테이블_동기화_위임() {
        Festival festival = festival(100L, null);
        Artist artist = artist(1L, "아이유");
        ArtistFestival af = ArtistFestival.builder().artist(artist).festival(festival).stageName("서브스테이지").build();
        given(artistFestivalRepository.findById(10L)).willReturn(Optional.of(af));

        service.updateArtistFestival(100L, 10L, new LineupUpdate("메인스테이지", null));

        assertThat(af.getStageName()).isEqualTo("메인스테이지");
        then(timetableSyncService).should().syncStage(100L, "아이유", "메인스테이지", "서브스테이지");
    }

    @Test
    void 참여정보_수정_날짜_변경시_타임테이블_동기화_위임() {
        Festival festival = festival(100L, null);
        Artist artist = artist(1L, "아이유");
        ArtistFestival af = ArtistFestival.builder().artist(artist).festival(festival)
                .performanceDate(LocalDate.of(2026, 8, 1)).build();
        given(artistFestivalRepository.findById(10L)).willReturn(Optional.of(af));

        service.updateArtistFestival(100L, 10L, new LineupUpdate(null, LocalDate.of(2026, 8, 3)));

        assertThat(af.getPerformanceDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        then(timetableSyncService).should().syncDate(100L, "아이유", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 1));
    }

    // ── updateArtistFestivalsBatch ──────────────────────────────────────────

    @Test
    void 라인업_일괄수정_빈맵이면_아무일도_안함() {
        ArtistFestivalService.BatchUpdateResult result = service.updateArtistFestivalsBatch(100L, Map.of());

        assertThat(result.success()).isEqualTo(0);
        assertThat(result.errors()).isEqualTo(0);
        then(artistFestivalRepository).shouldHaveNoInteractions();
    }

    @Test
    void 라인업_일괄수정_findById_반복_없이_findAllById로_한번에_조회() {
        Festival festival = festival(100L, null);
        ArtistFestival af1 = ArtistFestival.builder().artist(artist(1L, "아이유")).festival(festival).build();
        ArtistFestival af2 = ArtistFestival.builder().artist(artist(2L, "뉴진스")).festival(festival).build();
        ReflectionTestUtils.setField(af1, "id", 10L);
        ReflectionTestUtils.setField(af2, "id", 11L);
        // updateArtistFestivalsBatch는 Map.keySet()(Set)을 그대로 넘기므로 List가 아닌 Set으로 매칭해야 함
        given(artistFestivalRepository.findAllById(Set.of(10L, 11L))).willReturn(List.of(af1, af2));

        ArtistFestivalService.BatchUpdateResult result = service.updateArtistFestivalsBatch(100L, Map.of(
                10L, new LineupUpdate("메인스테이지", null),
                11L, new LineupUpdate("서브스테이지", null)));

        assertThat(result.success()).isEqualTo(2);
        assertThat(result.errors()).isEqualTo(0);
        then(artistFestivalRepository).should().findAllById(Set.of(10L, 11L));
        then(artistFestivalRepository).should(never()).findById(any());
    }

    @Test
    void 라인업_일괄수정_다른_페스티벌_행은_에러로_집계() {
        ArtistFestival wrongFestivalAf =
                artistFestival(artist(1L, "아이유"), festival(200L, null));
        given(artistFestivalRepository.findAllById(Set.of(10L))).willReturn(List.of(wrongFestivalAf));

        ArtistFestivalService.BatchUpdateResult result =
                service.updateArtistFestivalsBatch(100L, Map.of(10L, new LineupUpdate("메인스테이지", null)));

        assertThat(result.success()).isEqualTo(0);
        assertThat(result.errors()).isEqualTo(1);
    }

    // ── syncFromTimetableEntry ────────────────────────────────────────────

    @Test
    void 타임테이블에서_동기화_아티스트명_공백이면_무시() {
        service.syncFromTimetableEntry(100L, " ", new LineupUpdate("스테이지", LocalDate.now()));

        then(artistFestivalRepository).shouldHaveNoInteractions();
    }

    @Test
    void 타임테이블에서_동기화_참여정보_있으면_업데이트() {
        ArtistFestival af = artistFestival(artist(1L, "아이유"), festival(100L, null));
        given(artistFestivalRepository.findByFestivalIdAndArtistName(100L, "아이유")).willReturn(Optional.of(af));

        service.syncFromTimetableEntry(100L, "아이유", new LineupUpdate("메인스테이지", LocalDate.of(2026, 8, 1)));

        assertThat(af.getStageName()).isEqualTo("메인스테이지");
        assertThat(af.getPerformanceDate()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    void 타임테이블에서_동기화_참여정보_없으면_무시() {
        given(artistFestivalRepository.findByFestivalIdAndArtistName(100L, "아이유")).willReturn(Optional.empty());

        service.syncFromTimetableEntry(100L, "아이유", new LineupUpdate("메인스테이지", LocalDate.now()));
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
