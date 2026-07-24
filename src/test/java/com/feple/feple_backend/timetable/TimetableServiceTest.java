package com.feple.feple_backend.timetable;

import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.entity.LineupUpdate;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.stage.service.StageService;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequestDto;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponseDto;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import com.feple.feple_backend.timetable.service.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock TimetableRepository timetableRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock StageService stageService;
    @Mock ArtistFestivalService artistFestivalService;
    @Mock ArtistRepository artistRepository;

    @InjectMocks TimetableService timetableService;

    @Test
    void createEntry_종료시간_이전_예외() {
        Festival festival = mock(Festival.class);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(festival));

        TimetableEntryRequestDto req = new TimetableEntryRequestDto();
        req.setFestivalDate(LocalDate.of(2026, 7, 1));
        req.setStartTime(LocalTime.of(10, 0));
        req.setEndTime(LocalTime.of(9, 0));

        assertThatThrownBy(() -> timetableService.createEntry(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료 시간은 시작 시간보다 늦어야 합니다.");
    }

    @Test
    void createEntry_자정_넘기는_심야_공연_허용() {
        Festival festival = mock(Festival.class);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(festival));

        TimetableEntry savedEntry = TimetableEntry.builder()
                .festival(festival)
                .stageName("MAIN")
                .artistName("아티스트")
                .festivalDate(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(23, 30))
                .endTime(LocalTime.of(0, 30))
                .build();
        given(timetableRepository.save(any(TimetableEntry.class))).willReturn(savedEntry);
        given(stageService.findByFestivalIdAndName(eq(1L), eq("MAIN")))
                .willReturn(Optional.empty());

        TimetableEntryRequestDto req = new TimetableEntryRequestDto();
        req.setStageName("MAIN");
        req.setArtistName("아티스트");
        req.setFestivalDate(LocalDate.of(2026, 7, 1));
        req.setStartTime(LocalTime.of(23, 30));
        req.setEndTime(LocalTime.of(0, 30));

        timetableService.createEntry(1L, req);

        then(timetableRepository).should().save(any(TimetableEntry.class));
    }

    @Test
    void createEntry_빈_stageName_null_stage_처리() {
        Festival festival = mock(Festival.class);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(festival));

        TimetableEntry savedEntry = TimetableEntry.builder()
                .festival(festival)
                .stageName("")
                .artistName("아티스트")
                .festivalDate(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();
        given(timetableRepository.save(any(TimetableEntry.class))).willReturn(savedEntry);

        TimetableEntryRequestDto req = new TimetableEntryRequestDto();
        req.setStageName("  ");
        req.setArtistName("아티스트");
        req.setFestivalDate(LocalDate.of(2026, 7, 1));
        req.setStartTime(LocalTime.of(10, 0));
        req.setEndTime(LocalTime.of(12, 0));

        timetableService.createEntry(1L, req);

        then(stageService).should(never()).findByFestivalIdAndName(anyLong(), anyString());
    }

    @Test
    void createEntry_성공() {
        Festival festival = mock(Festival.class);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(festival));

        TimetableEntry savedEntry = TimetableEntry.builder()
                .festival(festival)
                .stageName("MAIN")
                .artistName("아티스트명")
                .festivalDate(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .build();
        given(timetableRepository.save(any(TimetableEntry.class))).willReturn(savedEntry);

        TimetableEntryRequestDto req = new TimetableEntryRequestDto();
        req.setStageName("MAIN");
        req.setArtistName("아티스트명");
        req.setFestivalDate(LocalDate.of(2026, 7, 1));
        req.setStartTime(LocalTime.of(14, 0));
        req.setEndTime(LocalTime.of(16, 0));

        given(stageService.findByFestivalIdAndName(eq(1L), eq("MAIN")))
                .willReturn(Optional.empty());

        TimetableEntryResponseDto response = timetableService.createEntry(1L, req);

        then(artistFestivalService).should().syncFromTimetableEntry(
                eq(1L), anyString(), any(LineupUpdate.class));
    }

    @Test
    void updateEntry_다른_페스티벌_예외() {
        Festival otherFestival = mock(Festival.class);
        given(otherFestival.getId()).willReturn(99L);

        TimetableEntry entry = TimetableEntry.builder()
                .festival(otherFestival)
                .stageName("")
                .artistName("아티스트")
                .festivalDate(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();
        given(timetableRepository.findById(5L)).willReturn(Optional.of(entry));

        TimetableEntryRequestDto req = new TimetableEntryRequestDto();
        req.setFestivalDate(LocalDate.of(2026, 7, 1));
        req.setStartTime(LocalTime.of(10, 0));
        req.setEndTime(LocalTime.of(12, 0));

        assertThatThrownBy(() -> timetableService.updateEntry(1L, 5L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 페스티벌의 항목이 아닙니다.");
    }

    @Test
    void updateEntry_시간_오류_예외() {
        Festival festival = mock(Festival.class);
        given(festival.getId()).willReturn(1L);

        TimetableEntry entry = TimetableEntry.builder()
                .festival(festival)
                .stageName("")
                .artistName("아티스트")
                .festivalDate(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();
        given(timetableRepository.findById(5L)).willReturn(Optional.of(entry));

        TimetableEntryRequestDto req = new TimetableEntryRequestDto();
        req.setFestivalDate(LocalDate.of(2026, 7, 1));
        req.setStartTime(LocalTime.of(12, 0));
        req.setEndTime(LocalTime.of(10, 0));

        assertThatThrownBy(() -> timetableService.updateEntry(1L, 5L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료 시간은 시작 시간보다 늦어야 합니다.");
    }

    @Test
    void deleteEntry_다른_페스티벌_예외() {
        Festival otherFestival = mock(Festival.class);
        given(otherFestival.getId()).willReturn(99L);

        TimetableEntry entry = TimetableEntry.builder()
                .festival(otherFestival)
                .stageName("")
                .artistName("아티스트")
                .festivalDate(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();
        given(timetableRepository.findById(5L)).willReturn(Optional.of(entry));

        assertThatThrownBy(() -> timetableService.deleteEntry(1L, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 페스티벌의 항목이 아닙니다.");
    }

    @Test
    void deleteEntry_성공() {
        Festival festival = mock(Festival.class);
        given(festival.getId()).willReturn(1L);

        TimetableEntry entry = TimetableEntry.builder()
                .festival(festival)
                .stageName("")
                .artistName("아티스트")
                .festivalDate(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(12, 0))
                .build();
        given(timetableRepository.findById(5L)).willReturn(Optional.of(entry));

        timetableService.deleteEntry(1L, 5L);

        then(timetableRepository).should().delete(entry);
    }
}
