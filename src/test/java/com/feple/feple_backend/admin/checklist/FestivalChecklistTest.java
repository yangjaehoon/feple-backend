package com.feple.feple_backend.admin.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FestivalChecklistTest {

    // ── of 팩토리 ─────────────────────────────────────────────────────────────

    @Test
    void of_festivalId_설정되고_모든_항목_false() {
        FestivalChecklist checklist = FestivalChecklist.of(42L);

        assertThat(checklist.getFestivalId()).isEqualTo(42L);
        assertThat(checklist.isAllCompleted(false)).isFalse();
        assertThat(checklist.isAllCompleted(true)).isFalse();
        assertThat(checklist.getCompletedCount(false)).isZero();
    }

    // ── toggle ────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} 토글 → true")
    @ValueSource(strings = {"lineup1", "lineup2", "lineup3", "boothMap"})
    void 알려진_항목_토글시_false에서_true로(String field) {
        FestivalChecklist checklist = FestivalChecklist.of(1L);

        checklist.toggle(field);

        assertThat(checklist.isChecked(field)).isTrue();
    }

    @Test
    void 두_번_토글하면_false로_복구() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);

        checklist.toggle("lineup1");
        checklist.toggle("lineup1");

        assertThat(checklist.isChecked("lineup1")).isFalse();
    }

    @Test
    void 알_수_없는_항목_토글시_예외() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);

        assertThatThrownBy(() -> checklist.toggle("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알 수 없는 항목");
    }

    // 타임테이블은 실데이터 기반으로 자동 계산되어 더 이상 수동 토글 대상이 아니다.
    @Test
    void timetable은_더이상_수동_토글_대상_아님() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);

        assertThatThrownBy(() -> checklist.toggle("timetable"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알 수 없는 항목");
    }

    // ── isChecked ───────────────────────────────────────────────────────────────

    @Test
    void isChecked_토글_전후_값_반영() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        assertThat(checklist.isChecked("boothMap")).isFalse();

        checklist.toggle("boothMap");

        assertThat(checklist.isChecked("boothMap")).isTrue();
    }

    @Test
    void isChecked_알_수_없는_항목은_false_반환() {
        assertThat(FestivalChecklist.of(1L).isChecked("nonexistent")).isFalse();
    }

    // ── getCompletedCount / isAllCompleted ────────────────────────────────────

    @Test
    void 아무것도_토글_안_하면_completedCount_0() {
        assertThat(FestivalChecklist.of(1L).getCompletedCount(false)).isZero();
    }

    @Test
    void 두_항목_토글하면_completedCount_2() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        checklist.toggle("lineup1");
        checklist.toggle("boothMap");

        assertThat(checklist.getCompletedCount(false)).isEqualTo(2);
    }

    @Test
    void timetableAutoComplete_true면_completedCount에_1_추가() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        checklist.toggle("lineup1");

        assertThat(checklist.getCompletedCount(true)).isEqualTo(2);
    }

    @Test
    void 모든_수동_항목_토글하고_timetableAutoComplete_true면_completedCount_5이고_isAllCompleted_true() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        for (ChecklistField f : ChecklistField.values()) {
            checklist.toggle(f.getKey());
        }

        assertThat(checklist.getCompletedCount(true)).isEqualTo(5);
        assertThat(checklist.isAllCompleted(true)).isTrue();
    }

    @Test
    void 모든_수동_항목_토글해도_timetableAutoComplete_false면_isAllCompleted_false() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        for (ChecklistField f : ChecklistField.values()) {
            checklist.toggle(f.getKey());
        }

        assertThat(checklist.isAllCompleted(false)).isFalse();
    }

    @Test
    void 일부만_토글되면_timetableAutoComplete_true여도_isAllCompleted_false() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        checklist.toggle("lineup1");
        checklist.toggle("lineup2");

        assertThat(checklist.isAllCompleted(true)).isFalse();
    }

    @Test
    void getFieldCount은_수동_4개_자동_1개_합쳐_5() {
        assertThat(FestivalChecklist.of(1L).getFieldCount()).isEqualTo(5);
    }

    // ── updateMemo ────────────────────────────────────────────────────────────

    @Test
    void updateMemo_메모_저장() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        checklist.updateMemo("내일 확인");

        assertThat(checklist.getMemo()).isEqualTo("내일 확인");
    }

    @Test
    void updateMemo_null_로_초기화() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        checklist.updateMemo("이전 메모");
        checklist.updateMemo(null);

        assertThat(checklist.getMemo()).isNull();
    }
}
