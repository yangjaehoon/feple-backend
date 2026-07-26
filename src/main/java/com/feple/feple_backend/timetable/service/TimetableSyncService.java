package com.feple.feple_backend.timetable.service;

import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.repository.StageRepository;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * ArtistFestival(아티스트 참여 정보)의 스테이지·날짜 변경을 타임테이블에 반영하는, 타임테이블 도메인 측 API.
 * ArtistFestivalService가 TimetableRepository/StageRepository를 직접 건드리지 않고 이 클래스를 통하도록 한다.
 * TimetableService에 두지 않은 이유: TimetableService가 이미 ArtistFestivalService를 의존하고 있어
 * (반대 방향 동기화 — broadcastLineupUpdate) 여기 메서드를 TimetableService에 추가하면 순환 의존이 생긴다.
 */
@Service
@RequiredArgsConstructor
public class TimetableSyncService {

    private final TimetableRepository timetableRepository;
    private final StageRepository stageRepository;

    @Transactional
    public void syncStage(Long festivalId, String artistName, String newStage, String oldStage) {
        if (newStage == null || newStage.equals(oldStage)) return;
        Stage stage = EntityLoader.getOrThrow(
                name -> stageRepository.findByFestivalIdAndName(festivalId, name), newStage, "스테이지");
        timetableRepository.findByFestivalIdAndArtistName(festivalId, artistName)
                .forEach(entry -> entry.updateStage(stage));
    }

    @Transactional
    public void syncDate(Long festivalId, String artistName, LocalDate newDate, LocalDate oldDate) {
        if (newDate == null || newDate.equals(oldDate)) return;
        List<TimetableEntry> entries = timetableRepository.findByFestivalIdAndArtistName(festivalId, artistName);
        if (oldDate != null) {
            entries.stream()
                    .filter(e -> oldDate.equals(e.getFestivalDate()))
                    .forEach(e -> e.updateDate(newDate));
        } else {
            entries.forEach(e -> e.updateDate(newDate));
        }
    }
}
