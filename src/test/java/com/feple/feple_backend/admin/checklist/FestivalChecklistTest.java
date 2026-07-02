package com.feple.feple_backend.admin.checklist;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FestivalChecklistTest {

    // ── of 팩토리 ─────────────────────────────────────────────────────────────

    @Test
    void of_festivalId_설정되고_모든_항목_false() {
        FestivalChecklist checklist = FestivalChecklist.of(42L);

        assertThat(checklist.getFestivalId()).isEqualTo(42L);
        assertThat(checklist.isAllCompleted()).isFalse();
        assertThat(checklist.getCompletedCount()).isZero();
    }

    // ── toggle ────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} 토글 → true")
    @ValueSource(strings = {"lineup1", "lineup2", "lineup3", "boothMap", "timetable"})
    void 알려진_항목_토글시_false에서_true로(String field) {
        FestivalChecklist checklist = FestivalChecklist.of(1L);

        checklist.toggle(field);

        assertThat(checklist.valueOf(field)).isTrue();
    }

    @Test
    void 두_번_토글하면_false로_복구() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);

        checklist.toggle("lineup1");
        checklist.toggle("lineup1");

        assertThat(checklist.valueOf("lineup1")).isFalse();
    }

    @Test
    void 알_수_없는_항목_토글시_예외() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);

        assertThatThrownBy(() -> checklist.toggle("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알 수 없는 항목");
    }

    // ── valueOf ───────────────────────────────────────────────────────────────

    @Test
    void valueOf_토글_전후_값_반영() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        assertThat(checklist.valueOf("boothMap")).isFalse();

        checklist.toggle("boothMap");

        assertThat(checklist.valueOf("boothMap")).isTrue();
    }

    @Test
    void valueOf_알_수_없는_항목은_false_반환() {
        assertThat(FestivalChecklist.of(1L).valueOf("nonexistent")).isFalse();
    }

    // ── getCompletedCount / isAllCompleted ────────────────────────────────────

    @Test
    void 아무것도_토글_안_하면_completedCount_0() {
        assertThat(FestivalChecklist.of(1L).getCompletedCount()).isZero();
    }

    @Test
    void 두_항목_토글하면_completedCount_2() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        checklist.toggle("lineup1");
        checklist.toggle("timetable");

        assertThat(checklist.getCompletedCount()).isEqualTo(2);
    }

    @Test
    void 모든_항목_토글하면_completedCount_5이고_isAllCompleted_true() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        for (String field : FestivalChecklist.ALL_FIELDS) {
            checklist.toggle(field);
        }

        assertThat(checklist.getCompletedCount()).isEqualTo(5);
        assertThat(checklist.isAllCompleted()).isTrue();
    }

    @Test
    void 일부만_토글되면_isAllCompleted_false() {
        FestivalChecklist checklist = FestivalChecklist.of(1L);
        checklist.toggle("lineup1");
        checklist.toggle("lineup2");

        assertThat(checklist.isAllCompleted()).isFalse();
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
