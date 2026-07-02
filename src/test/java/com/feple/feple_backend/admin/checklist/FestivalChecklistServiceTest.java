package com.feple.feple_backend.admin.checklist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FestivalChecklistServiceTest {

    @Mock FestivalChecklistRepository checklistRepository;

    @InjectMocks FestivalChecklistService service;

    // ── getChecklistMap ───────────────────────────────────────────────────────

    @Test
    void getChecklistMap_festivalId를_키로_변환() {
        FestivalChecklist c1 = FestivalChecklist.of(1L);
        FestivalChecklist c2 = FestivalChecklist.of(2L);
        given(checklistRepository.findByFestivalIdIn(List.of(1L, 2L))).willReturn(List.of(c1, c2));

        Map<Long, FestivalChecklist> result = service.getChecklistMap(List.of(1L, 2L));

        assertThat(result).containsEntry(1L, c1).containsEntry(2L, c2);
    }

    @Test
    void getChecklistMap_빈_입력이면_빈_맵_반환() {
        given(checklistRepository.findByFestivalIdIn(List.of())).willReturn(List.of());

        assertThat(service.getChecklistMap(List.of())).isEmpty();
    }

    // ── toggle ────────────────────────────────────────────────────────────────

    @Test
    void toggle_기존_체크리스트_있으면_저장_없이_토글() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        given(checklistRepository.findByFestivalId(1L)).willReturn(Optional.of(checklist));

        service.toggle(1L, "lineup1");

        assertThat(checklist.valueOf("lineup1")).isTrue();
        verify(checklistRepository, never()).save(any());
    }

    @Test
    void toggle_체크리스트_없으면_새로_저장_후_토글() {
        FestivalChecklist newChecklist = FestivalChecklist.of(1L);
        given(checklistRepository.findByFestivalId(1L)).willReturn(Optional.empty());
        given(checklistRepository.save(any())).willReturn(newChecklist);

        service.toggle(1L, "boothMap");

        assertThat(newChecklist.valueOf("boothMap")).isTrue();
        verify(checklistRepository).save(any());
    }

    @Test
    void toggle_알_수_없는_항목은_예외_전파() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        given(checklistRepository.findByFestivalId(1L)).willReturn(Optional.of(checklist));

        assertThatThrownBy(() -> service.toggle(1L, "unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알 수 없는 항목");
    }

    // ── isChecked ─────────────────────────────────────────────────────────────

    @Test
    void isChecked_항목이_true이면_true_반환() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        checklist.toggle("timetable");
        given(checklistRepository.findByFestivalId(1L)).willReturn(Optional.of(checklist));

        assertThat(service.isChecked(1L, "timetable")).isTrue();
    }

    @Test
    void isChecked_항목이_false이면_false_반환() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        given(checklistRepository.findByFestivalId(1L)).willReturn(Optional.of(checklist));

        assertThat(service.isChecked(1L, "lineup1")).isFalse();
    }

    @Test
    void isChecked_체크리스트_없으면_false_반환() {
        given(checklistRepository.findByFestivalId(99L)).willReturn(Optional.empty());

        assertThat(service.isChecked(99L, "lineup1")).isFalse();
    }

    // ── saveMemo ──────────────────────────────────────────────────────────────

    @Test
    void saveMemo_기존_체크리스트_있으면_저장_없이_메모_갱신() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        given(checklistRepository.findByFestivalId(1L)).willReturn(Optional.of(checklist));

        service.saveMemo(1L, "확인 완료");

        assertThat(checklist.getMemo()).isEqualTo("확인 완료");
        verify(checklistRepository, never()).save(any());
    }

    @Test
    void saveMemo_체크리스트_없으면_새로_저장_후_메모_갱신() {
        FestivalChecklist newChecklist = FestivalChecklist.of(1L);
        given(checklistRepository.findByFestivalId(1L)).willReturn(Optional.empty());
        given(checklistRepository.save(any())).willReturn(newChecklist);

        service.saveMemo(1L, "신규 메모");

        assertThat(newChecklist.getMemo()).isEqualTo("신규 메모");
        verify(checklistRepository).save(any());
    }
}
