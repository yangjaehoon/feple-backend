package com.feple.feple_backend.admin.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalChecklistServiceTest {

    @Mock FestivalChecklistRepository checklistRepository;
    @Mock FestivalRepository festivalRepository;

    @InjectMocks FestivalChecklistService service;

    // 체크리스트를 새로 만드는 경로는 festival_checklist → festival FK 때문에 페스티벌 존재를 먼저 확인한다.
    private void givenFestivalExists(Long festivalId) {
        given(festivalRepository.existsById(festivalId)).willReturn(true);
    }

    // ── getChecklistMap ───────────────────────────────────────────────────────

    @Test
    void getChecklistMap_festivalId를_키로_변환() {
        FestivalChecklist c1 = FestivalChecklist.of(1L);
        FestivalChecklist c2 = FestivalChecklist.of(2L);
        given(checklistRepository.findAll()).willReturn(List.of(c1, c2));

        Map<Long, FestivalChecklist> result = service.getChecklistMap();

        assertThat(result).containsEntry(1L, c1).containsEntry(2L, c2);
    }

    @Test
    void getChecklistMap_빈_입력이면_빈_맵_반환() {
        given(checklistRepository.findAll()).willReturn(List.of());

        assertThat(service.getChecklistMap()).isEmpty();
    }

    // ── toggle ────────────────────────────────────────────────────────────────

    @Test
    void toggle_기존_체크리스트_있으면_저장_없이_토글() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        given(checklistRepository.findByFestivalId(1L)).willReturn(Optional.of(checklist));

        boolean newValue = service.toggle(1L, "lineup1");

        assertThat(newValue).isTrue();
        assertThat(checklist.isChecked("lineup1")).isTrue();
        verify(checklistRepository, never()).save(any());
    }

    @Test
    void toggle_체크리스트_없으면_새로_저장_후_토글() {
        FestivalChecklist newChecklist = FestivalChecklist.of(1L);
        given(checklistRepository.findByFestivalId(1L)).willReturn(Optional.empty());
        givenFestivalExists(1L);
        given(checklistRepository.save(any())).willReturn(newChecklist);

        boolean newValue = service.toggle(1L, "boothMap");

        assertThat(newValue).isTrue();
        assertThat(newChecklist.isChecked("boothMap")).isTrue();
        verify(checklistRepository).save(any());
    }

    // 예전에는 여기서 "save()의 유니크 제약 위반을 catch해 재조회하면 토글이 유실되지 않는다"를
    // 검증했지만, 그 복구는 실제로는 동작하지 않았다 — save()는 자체 @Transactional을 가진 프록시
    // 호출이라 예외가 밖으로 나온 시점에 트랜잭션이 rollback-only로 마킹되고, catch해도 커밋이
    // 통째로 실패한다. 리포지토리를 목킹하는 단위 테스트라 그 사실이 드러나지 않았다.
    // 지금은 복구를 시도하지 않고, 도달 가능한 실패인 "존재하지 않는 페스티벌"만 명확히 처리한다.
    @Test
    void toggle_존재하지_않는_페스티벌이면_저장하지_않고_예외() {
        given(checklistRepository.findByFestivalId(99L)).willReturn(Optional.empty());
        given(festivalRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.toggle(99L, "lineup1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("페스티벌");
        verify(checklistRepository, never()).save(any());
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
        checklist.toggle("boothMap");
        given(checklistRepository.findByFestivalId(1L)).willReturn(Optional.of(checklist));

        assertThat(service.isChecked(1L, "boothMap")).isTrue();
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
        givenFestivalExists(1L);
        given(checklistRepository.save(any())).willReturn(newChecklist);

        service.saveMemo(1L, "신규 메모");

        assertThat(newChecklist.getMemo()).isEqualTo("신규 메모");
        verify(checklistRepository).save(any());
    }
}
