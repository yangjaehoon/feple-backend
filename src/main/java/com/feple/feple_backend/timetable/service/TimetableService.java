package com.feple.feple_backend.timetable.service;

import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.repository.StageRepository;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequest;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponse;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feple.feple_backend.global.EntityFinder;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final FestivalRepository festivalRepository;
    private final StageRepository stageRepository;
    private final ArtistFestivalService artistFestivalService;

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> getEntries(Long festivalId) {
        return timetableRepository.findByFestivalIdWithStage(festivalId)
                .stream()
                .map(TimetableEntryResponse::from)
                .sorted(Comparator
                        .comparing(TimetableEntryResponse::getFestivalDate)
                        .thenComparingInt(TimetableEntryResponse::getStageOrder)
                        .thenComparing(TimetableEntryResponse::getStartTime))
                .toList();
    }

    @Transactional
    public TimetableEntryResponse createEntry(Long festivalId, TimetableEntryRequest req) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }
        String rawStageName = req.getStageName();
        String stageName = (rawStageName == null || rawStageName.isBlank()) ? "" : rawStageName.trim();
        Stage stage = stageName.isEmpty() ? null : stageRepository.findByFestivalIdAndName(festivalId, stageName).orElse(null);

        TimetableEntry entry = TimetableEntry.builder()
                .festival(festival)
                .stage(stage)
                .stageName(stageName)
                .artistName(req.getArtistName() != null ? req.getArtistName().trim() : "")
                .festivalDate(req.getFestivalDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .build();
        TimetableEntry saved = timetableRepository.save(entry);
        artistFestivalService.syncFromTimetableEntry(
                festivalId, saved.getArtistName(), saved.getFestivalDate(), saved.getStageName());
        return TimetableEntryResponse.from(saved);
    }

    @Transactional
    public void updateEntry(Long festivalId, Long entryId, TimetableEntryRequest req) {
        TimetableEntry entry = EntityFinder.getOrThrow(timetableRepository::findById, entryId, "타임테이블 항목");
        if (!festivalId.equals(entry.getFestivalId())) {
            throw new IllegalArgumentException("해당 페스티벌의 항목이 아닙니다.");
        }
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }
        String stageName = req.getStageName() == null ? "" : req.getStageName().trim();
        Stage stage = stageName.isEmpty() ? null
                : stageRepository.findByFestivalIdAndName(festivalId, stageName).orElse(null);
        entry.update(
                req.getArtistName() != null ? req.getArtistName().trim() : "",
                stageName,
                stage,
                req.getFestivalDate(),
                req.getStartTime(),
                req.getEndTime());
        artistFestivalService.syncFromTimetableEntry(
                festivalId, entry.getArtistName(), entry.getFestivalDate(), entry.getStageName());
    }

    @Transactional
    public void nullifyArtistId(Long artistId) {
        timetableRepository.nullifyArtistId(artistId);
    }

    @Transactional
    public void deleteEntry(Long festivalId, Long entryId) {
        TimetableEntry entry = EntityFinder.getOrThrow(timetableRepository::findById, entryId, "타임테이블 항목");
        if (!festivalId.equals(entry.getFestivalId())) {
            throw new IllegalArgumentException("해당 페스티벌의 항목이 아닙니다.");
        }
        timetableRepository.delete(entry);
    }
}
