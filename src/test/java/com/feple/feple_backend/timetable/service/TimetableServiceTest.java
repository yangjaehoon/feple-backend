package com.feple.feple_backend.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.service.StageService;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequestDto;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponseDto;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock TimetableRepository timetableRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock StageService stageService;
    @Mock ArtistFestivalService artistFestivalService;
    @Mock ArtistRepository artistRepository;
    @Mock TimetableEntryBatchPersister entryBatchPersister;

    @InjectMocks TimetableService timetableService;

    private Festival festival(Long id) {
        return Festival.builder().id(id).title("락페").build();
    }

    private TimetableEntry entry(Long id, Festival festival, String artistName, String stageName) {
        return TimetableEntry.builder()
                .id(id).festival(festival).artistName(artistName).stageName(stageName)
                .festivalDate(LocalDate.of(2026, 8, 1))
                .startTime(LocalTime.of(19, 0)).endTime(LocalTime.of(20, 0))
                .build();
    }

    private TimetableEntryRequestDto requestDto(String artistName, String stageName, LocalTime start, LocalTime end) {
        TimetableEntryRequestDto dto = new TimetableEntryRequestDto();
        dto.setArtistName(artistName);
        dto.setStageName(stageName);
        dto.setFestivalDate(LocalDate.of(2026, 8, 1));
        dto.setStartTime(start);
        dto.setEndTime(end);
        return dto;
    }

    // ── getEntries ────────────────────────────────────────────────────

    @Test
    void 타임테이블_조회시_날짜_스테이지순서_시작시간순_정렬() {
        Festival f = festival(1L);
        TimetableEntry late = TimetableEntry.builder()
                .festival(f).stageName("메인").artistName("늦은공연")
                .festivalDate(LocalDate.of(2026, 8, 1))
                .startTime(LocalTime.of(15, 0)).endTime(LocalTime.of(16, 0)).build();
        TimetableEntry early = TimetableEntry.builder()
                .festival(f).stageName("메인").artistName("이른공연")
                .festivalDate(LocalDate.of(2026, 8, 1))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).build();
        TimetableEntry nextDay = TimetableEntry.builder()
                .festival(f).stageName("메인").artistName("다음날공연")
                .festivalDate(LocalDate.of(2026, 8, 2))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).build();
        given(timetableRepository.findByFestivalIdWithStage(1L)).willReturn(List.of(late, nextDay, early));

        List<TimetableEntryResponseDto> result = timetableService.getEntries(1L);

        assertThat(result).extracting(TimetableEntryResponseDto::getArtistName)
                .containsExactly("이른공연", "늦은공연", "다음날공연");
    }

    @Test
    void 스테이지명_공백이면_null_스테이지로_처리() {
        Festival f = festival(1L);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        given(timetableRepository.save(any(TimetableEntry.class))).willAnswer(inv -> inv.getArgument(0));
        TimetableEntryRequestDto dto = requestDto("아이유", "  ", LocalTime.of(19, 0), LocalTime.of(20, 0));

        timetableService.createEntry(1L, dto);

        verify(stageService, never()).findByFestivalIdAndName(any(), any());
    }

    // ── createEntry ───────────────────────────────────────────────────

    @Test
    void 존재하지_않는_페스티벌에_항목_생성시_예외() {
        given(festivalRepository.findById(99L)).willReturn(Optional.empty());
        TimetableEntryRequestDto dto = requestDto("아이유", "메인", LocalTime.of(19, 0), LocalTime.of(20, 0));

        assertThatThrownBy(() -> timetableService.createEntry(99L, dto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 종료시간이_시작시간보다_빠르면_예외() {
        Festival f = festival(1L);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        TimetableEntryRequestDto dto = requestDto("아이유", "메인", LocalTime.of(20, 0), LocalTime.of(19, 0));

        assertThatThrownBy(() -> timetableService.createEntry(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료 시간은 시작 시간보다 늦어야 합니다.");
    }

    @Test
    void Festival을_직접_전달하면_festivalRepository_재조회_없이_생성() {
        // OCR 일괄 적용처럼 같은 festival로 여러 엔트리를 연속 생성할 때 매 호출마다
        // festivalRepository.findById를 다시 타지 않는지 확인 (N+1 방지 회귀 테스트)
        Festival f = festival(1L);
        TimetableEntryRequestDto dto = requestDto("아이유", "", LocalTime.of(19, 0), LocalTime.of(20, 0));
        given(timetableRepository.save(any(TimetableEntry.class))).willAnswer(inv -> inv.getArgument(0));

        TimetableEntryResponseDto result = timetableService.createEntry(f, dto);

        assertThat(result).isNotNull();
        verify(festivalRepository, never()).findById(any());
    }

    @Test
    void 심야공연_시간대는_역전되어도_허용() {
        Festival f = festival(1L);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        TimetableEntryRequestDto dto = requestDto("아이유", "메인", LocalTime.of(23, 30), LocalTime.of(0, 30));
        given(timetableRepository.save(any(TimetableEntry.class))).willAnswer(inv -> inv.getArgument(0));

        TimetableEntryResponseDto result = timetableService.createEntry(1L, dto);

        assertThat(result).isNotNull();
    }

    @Test
    void 저녁_이른시각_시작하는_심야공연도_허용() {
        // 임계값(정오)만 넘으면 심야 공연으로 인정 — 18시 이전 시작이라고 거부하지 않는다
        Festival f = festival(1L);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        TimetableEntryRequestDto dto = requestDto("아이유", "메인", LocalTime.of(17, 59), LocalTime.of(0, 30));
        given(timetableRepository.save(any(TimetableEntry.class))).willAnswer(inv -> inv.getArgument(0));

        TimetableEntryResponseDto result = timetableService.createEntry(1L, dto);

        assertThat(result).isNotNull();
    }

    @Test
    void 정오_이전_시작에_종료가_이른시각이면_역전으로_거부() {
        // 오전 공연에서 시작/종료를 실수로 바꿔 입력한 경우까지 심야 공연으로 오인하지 않는다
        Festival f = festival(1L);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        TimetableEntryRequestDto dto = requestDto("아이유", "메인", LocalTime.of(11, 0), LocalTime.of(10, 0));

        assertThatThrownBy(() -> timetableService.createEntry(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료 시간은 시작 시간보다 늦어야 합니다.");
    }

    @Test
    void 스테이지명_있으면_해당_스테이지_조회후_연결() {
        Festival f = festival(1L);
        Stage stage = Stage.builder().id(5L).name("메인스테이지").build();
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        given(stageService.findByFestivalIdAndName(1L, "메인스테이지")).willReturn(Optional.of(stage));
        given(timetableRepository.save(any(TimetableEntry.class))).willAnswer(inv -> inv.getArgument(0));
        TimetableEntryRequestDto dto = requestDto("아이유", "메인스테이지", LocalTime.of(19, 0), LocalTime.of(20, 0));

        timetableService.createEntry(1L, dto);

        verify(stageService).findByFestivalIdAndName(1L, "메인스테이지");
    }

    @Test
    void 스테이지명_없으면_스테이지_조회_스킵() {
        Festival f = festival(1L);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        given(timetableRepository.save(any(TimetableEntry.class))).willAnswer(inv -> inv.getArgument(0));
        TimetableEntryRequestDto dto = requestDto("아이유", null, LocalTime.of(19, 0), LocalTime.of(20, 0));

        timetableService.createEntry(1L, dto);

        verify(stageService, never()).findByFestivalIdAndName(any(), any());
    }

    @Test
    void 멤버아티스트ID_있으면_존재하는_아티스트만_연결() {
        Festival f = festival(1L);
        Artist a1 = Artist.builder().id(10L).name("멤버1").build();
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        given(artistRepository.findAllById(List.of(10L, 20L))).willReturn(List.of(a1));
        given(timetableRepository.save(any(TimetableEntry.class))).willAnswer(inv -> inv.getArgument(0));
        TimetableEntryRequestDto dto = requestDto("팀", null, LocalTime.of(19, 0), LocalTime.of(20, 0));
        dto.setMemberArtistIds(List.of(10L, 20L));

        TimetableEntryResponseDto result = timetableService.createEntry(1L, dto);

        assertThat(result.getMemberArtistNames()).containsExactly("멤버1");
    }

    @Test
    void 멤버아티스트ID_없으면_멤버_비움() {
        Festival f = festival(1L);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        given(timetableRepository.save(any(TimetableEntry.class))).willAnswer(inv -> inv.getArgument(0));
        TimetableEntryRequestDto dto = requestDto("아이유", null, LocalTime.of(19, 0), LocalTime.of(20, 0));

        TimetableEntryResponseDto result = timetableService.createEntry(1L, dto);

        assertThat(result.getMemberArtistNames()).isEmpty();
        verify(artistRepository, never()).findAllById(any());
    }

    @Test
    void 항목_생성시_라인업_동기화_이벤트_전파() {
        Festival f = festival(1L);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        given(timetableRepository.save(any(TimetableEntry.class))).willAnswer(inv -> inv.getArgument(0));
        TimetableEntryRequestDto dto = requestDto("아이유", "메인", LocalTime.of(19, 0), LocalTime.of(20, 0));

        timetableService.createEntry(1L, dto);

        verify(artistFestivalService).syncFromTimetableEntry(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("아이유"), any());
    }

    // ── createEntriesBatch ───────────────────────────────────────────────

    @Test
    void 배치_생성시_항목마다_격리된_트랜잭션으로_저장() {
        Festival f = festival(1L);
        TimetableEntryRequestDto dto1 = requestDto("아이유", "메인", LocalTime.of(19, 0), LocalTime.of(20, 0));
        TimetableEntryRequestDto dto2 = requestDto("뉴진스", "메인", LocalTime.of(20, 0), LocalTime.of(21, 0));
        given(stageService.getStages(1L)).willReturn(List.of());
        TimetableEntry saved1 = entry(10L, f, "아이유", "메인");
        TimetableEntry saved2 = entry(11L, f, "뉴진스", "메인");
        given(entryBatchPersister.saveIsolated(f, null, "메인", dto1)).willReturn(saved1);
        given(entryBatchPersister.saveIsolated(f, null, "메인", dto2)).willReturn(saved2);

        List<TimetableService.BatchCreateResult> results =
                timetableService.createEntriesBatch(f, List.of(dto1, dto2));

        assertThat(results).hasSize(2);
        assertThat(results).extracting(TimetableService.BatchCreateResult::entry)
                .containsExactly(saved1, saved2);
        verify(entryBatchPersister).saveIsolated(f, null, "메인", dto1);
        verify(entryBatchPersister).saveIsolated(f, null, "메인", dto2);
    }

    @Test
    void 배치_생성시_한_항목_저장실패해도_나머지는_계속_처리() {
        // entryBatchPersister가 항목마다 독립 트랜잭션이라, 한 건이 예외를 던져도
        // 이후 항목의 저장 자체가 영향받지 않아야 한다 (세션 오염 격리 회귀 테스트)
        Festival f = festival(1L);
        TimetableEntryRequestDto dto1 = requestDto("실패아티스트", "메인", LocalTime.of(19, 0), LocalTime.of(20, 0));
        TimetableEntryRequestDto dto2 = requestDto("성공아티스트", "메인", LocalTime.of(20, 0), LocalTime.of(21, 0));
        given(stageService.getStages(1L)).willReturn(List.of());
        RuntimeException dbError = new RuntimeException("제약 위반");
        given(entryBatchPersister.saveIsolated(f, null, "메인", dto1)).willThrow(dbError);
        TimetableEntry saved2 = entry(11L, f, "성공아티스트", "메인");
        given(entryBatchPersister.saveIsolated(f, null, "메인", dto2)).willReturn(saved2);

        List<TimetableService.BatchCreateResult> results =
                timetableService.createEntriesBatch(f, List.of(dto1, dto2));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).entry()).isNull();
        assertThat(results.get(0).error()).isEqualTo(dbError);
        assertThat(results.get(1).entry()).isEqualTo(saved2);
        assertThat(results.get(1).error()).isNull();
    }

    @Test
    void 배치_생성시_시간역전_검증실패는_저장시도조차_안함() {
        Festival f = festival(1L);
        TimetableEntryRequestDto invalidDto = requestDto("아이유", "메인", LocalTime.of(20, 0), LocalTime.of(19, 0));
        given(stageService.getStages(1L)).willReturn(List.of());

        List<TimetableService.BatchCreateResult> results =
                timetableService.createEntriesBatch(f, List.of(invalidDto));

        assertThat(results.get(0).entry()).isNull();
        assertThat(results.get(0).error()).isInstanceOf(IllegalArgumentException.class);
        verify(entryBatchPersister, never()).saveIsolated(any(), any(), any(), any());
    }

    // ── updateEntry ───────────────────────────────────────────────────

    @Test
    void 항목_수정_성공() {
        Festival f = festival(1L);
        TimetableEntry existing = entry(100L, f, "기존", "메인");
        given(timetableRepository.findById(100L)).willReturn(Optional.of(existing));
        TimetableEntryRequestDto dto = requestDto("수정된아티스트", "메인", LocalTime.of(19, 0), LocalTime.of(20, 0));

        timetableService.updateEntry(1L, 100L, dto);

        assertThat(existing.getArtistName()).isEqualTo("수정된아티스트");
    }

    @Test
    void 다른_페스티벌_소속_항목_수정시_예외() {
        Festival f = festival(1L);
        TimetableEntry existing = entry(100L, f, "기존", "메인");
        given(timetableRepository.findById(100L)).willReturn(Optional.of(existing));
        TimetableEntryRequestDto dto = requestDto("수정", "메인", LocalTime.of(19, 0), LocalTime.of(20, 0));

        assertThatThrownBy(() -> timetableService.updateEntry(99L, 100L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 페스티벌의 항목이 아닙니다.");
    }

    @Test
    void 존재하지_않는_항목_수정시_예외() {
        given(timetableRepository.findById(999L)).willReturn(Optional.empty());
        TimetableEntryRequestDto dto = requestDto("수정", "메인", LocalTime.of(19, 0), LocalTime.of(20, 0));

        assertThatThrownBy(() -> timetableService.updateEntry(1L, 999L, dto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 항목_수정시_종료시간이_시작시간보다_빠르면_예외() {
        Festival f = festival(1L);
        TimetableEntry existing = entry(100L, f, "기존", "메인");
        given(timetableRepository.findById(100L)).willReturn(Optional.of(existing));
        TimetableEntryRequestDto dto = requestDto("수정", "메인", LocalTime.of(20, 0), LocalTime.of(19, 0));

        assertThatThrownBy(() -> timetableService.updateEntry(1L, 100L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료 시간은 시작 시간보다 늦어야 합니다.");
    }

    // ── deleteEntry ───────────────────────────────────────────────────

    @Test
    void 항목_삭제_성공() {
        Festival f = festival(1L);
        TimetableEntry existing = entry(100L, f, "아티스트", "메인");
        given(timetableRepository.findById(100L)).willReturn(Optional.of(existing));

        timetableService.deleteEntry(1L, 100L);

        verify(timetableRepository).delete(existing);
    }

    @Test
    void 다른_페스티벌_소속_항목_삭제시_예외() {
        Festival f = festival(1L);
        TimetableEntry existing = entry(100L, f, "아티스트", "메인");
        given(timetableRepository.findById(100L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> timetableService.deleteEntry(99L, 100L))
                .isInstanceOf(IllegalArgumentException.class);
        verify(timetableRepository, never()).delete(any());
    }

    // ── removeAllByFestival ───────────────────────────────────────────

    @Test
    void 페스티벌_전체_타임테이블_삭제() {
        timetableService.removeAllByFestival(1L);

        verify(timetableRepository).deleteByFestivalId(1L);
    }
}
