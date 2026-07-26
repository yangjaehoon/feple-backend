package com.feple.feple_backend.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.repository.StageRepository;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimetableSyncServiceTest {

    @Mock TimetableRepository timetableRepository;
    @Mock StageRepository stageRepository;

    @InjectMocks TimetableSyncService service;

    // ── syncStage ────────────────────────────────────────────────────────

    @Test
    void 스테이지_변경시_타임테이블_동기화() {
        Stage newStage = mock(Stage.class);
        given(newStage.getName()).willReturn("메인스테이지");
        given(stageRepository.findByFestivalIdAndName(100L, "메인스테이지")).willReturn(Optional.of(newStage));

        TimetableEntry entry = TimetableEntry.builder()
                .artistName("아이유").festivalDate(LocalDate.of(2026, 8, 1)).build();
        given(timetableRepository.findByFestivalIdAndArtistName(100L, "아이유")).willReturn(List.of(entry));

        service.syncStage(100L, "아이유", "메인스테이지", "서브스테이지");

        assertThat(entry.getStageName()).isEqualTo("메인스테이지");
    }

    @Test
    void 새_스테이지가_없으면_동기화_안함() {
        service.syncStage(100L, "아이유", null, "서브스테이지");

        then(timetableRepository).shouldHaveNoInteractions();
        then(stageRepository).shouldHaveNoInteractions();
    }

    @Test
    void 스테이지가_기존값과_같으면_동기화_안함() {
        service.syncStage(100L, "아이유", "메인스테이지", "메인스테이지");

        then(timetableRepository).shouldHaveNoInteractions();
        then(stageRepository).shouldHaveNoInteractions();
    }

    // ── syncDate ─────────────────────────────────────────────────────────

    @Test
    void 날짜_변경시_기존날짜_있으면_해당날짜_항목만_동기화() {
        TimetableEntry sameDate = TimetableEntry.builder()
                .artistName("아이유").festivalDate(LocalDate.of(2026, 8, 1)).build();
        TimetableEntry otherDate = TimetableEntry.builder()
                .artistName("아이유").festivalDate(LocalDate.of(2026, 8, 2)).build();
        given(timetableRepository.findByFestivalIdAndArtistName(100L, "아이유"))
                .willReturn(List.of(sameDate, otherDate));

        service.syncDate(100L, "아이유", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 1));

        assertThat(sameDate.getFestivalDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(otherDate.getFestivalDate()).isEqualTo(LocalDate.of(2026, 8, 2));
    }

    @Test
    void 날짜_변경시_기존날짜_없으면_전체_동기화() {
        TimetableEntry entry1 = TimetableEntry.builder().artistName("아이유").festivalDate(LocalDate.of(2026, 8, 1)).build();
        TimetableEntry entry2 = TimetableEntry.builder().artistName("아이유").festivalDate(LocalDate.of(2026, 8, 2)).build();
        given(timetableRepository.findByFestivalIdAndArtistName(100L, "아이유"))
                .willReturn(List.of(entry1, entry2));

        service.syncDate(100L, "아이유", LocalDate.of(2026, 8, 3), null);

        assertThat(entry1.getFestivalDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(entry2.getFestivalDate()).isEqualTo(LocalDate.of(2026, 8, 3));
    }

    @Test
    void 새_날짜가_없으면_동기화_안함() {
        service.syncDate(100L, "아이유", null, LocalDate.of(2026, 8, 1));

        then(timetableRepository).shouldHaveNoInteractions();
    }
}
