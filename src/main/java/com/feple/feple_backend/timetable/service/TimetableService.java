package com.feple.feple_backend.timetable.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.entity.LineupUpdate;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.service.StageService;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequestDto;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponseDto;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.entity.TimetableEntryFields;
import com.feple.feple_backend.timetable.entity.TimetableEntryMember;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feple.feple_backend.global.EntityLoader;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final FestivalRepository festivalRepository;
    private final StageService stageService;
    private final ArtistFestivalService artistFestivalService;
    private final ArtistRepository artistRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "timetable", key = "#festivalId")
    public List<TimetableEntryResponseDto> getEntries(Long festivalId) {
        return timetableRepository.findByFestivalIdWithStage(festivalId)
                .stream()
                .map(TimetableEntryResponseDto::from)
                .sorted(Comparator
                        .comparing(TimetableEntryResponseDto::getFestivalDate)
                        .thenComparingInt(TimetableEntryResponseDto::getStageOrder)
                        .thenComparing(TimetableEntryResponseDto::getStartTime))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "timetable", key = "#festivalId")
    public TimetableEntryResponseDto createEntry(Long festivalId, TimetableEntryRequestDto req) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        validateTimeRange(req);
        String rawStageName = req.getStageName();
        String stageName = (rawStageName == null || rawStageName.isBlank()) ? "" : rawStageName.trim();
        Stage stage = stageName.isEmpty() ? null : stageService.findByFestivalIdAndName(festivalId, stageName).orElse(null);

        String color = (req.getColor() != null && !req.getColor().isBlank()) ? req.getColor().trim() : null;
        TimetableEntry entry = TimetableEntry.builder()
                .festival(festival)
                .stage(stage)
                .stageName(stageName)
                .artistName(req.getArtistName() != null ? req.getArtistName().trim() : "")
                .festivalDate(req.getFestivalDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .color(color)
                .build();
        TimetableEntry saved = timetableRepository.save(entry);
        syncMembers(saved, req.getMemberArtistIds());
        broadcastLineupUpdate(festivalId, saved);
        return TimetableEntryResponseDto.from(saved);
    }

    @Transactional
    @CacheEvict(value = "timetable", key = "#festivalId")
    public void updateEntry(Long festivalId, Long entryId, TimetableEntryRequestDto req) {
        TimetableEntry entry = EntityLoader.getOrThrow(timetableRepository::findById, entryId, "타임테이블 항목");
        if (!festivalId.equals(entry.getFestivalId())) {
            throw new IllegalArgumentException("해당 페스티벌의 항목이 아닙니다.");
        }
        validateTimeRange(req);
        String stageName = req.getStageName() == null ? "" : req.getStageName().trim();
        Stage stage = stageName.isEmpty() ? null
                : stageService.findByFestivalIdAndName(festivalId, stageName).orElse(null);
        entry.update(new TimetableEntryFields(
                req.getArtistName() != null ? req.getArtistName().trim() : "",
                stageName,
                stage,
                req.getFestivalDate(),
                req.getStartTime(),
                req.getEndTime(),
                req.getColor()));
        syncMembers(entry, req.getMemberArtistIds());
        broadcastLineupUpdate(festivalId, entry);
    }

    private void validateTimeRange(TimetableEntryRequestDto req) {
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }
    }

    private void broadcastLineupUpdate(Long festivalId, TimetableEntry entry) {
        LineupUpdate lineup = new LineupUpdate(entry.getStageName(), entry.getFestivalDate());
        artistFestivalService.syncFromTimetableEntry(festivalId, entry.getArtistName(), lineup);
        for (TimetableEntryMember member : entry.getMembers()) {
            artistFestivalService.syncFromTimetableEntry(festivalId, member.getArtistName(), lineup);
        }
    }

    private void syncMembers(TimetableEntry entry, List<Long> memberArtistIds) {
        if (memberArtistIds == null || memberArtistIds.isEmpty()) {
            entry.replaceMembers(List.of());
            return;
        }
        List<TimetableEntryMember> members = memberArtistIds.stream()
                .map(artistRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(artist -> TimetableEntryMember.builder()
                        .entry(entry)
                        .artist(artist)
                        .artistName(artist.getName())
                        .build())
                .toList();
        entry.replaceMembers(members);
    }

    @Transactional
    public void nullifyArtistId(Long artistId) {
        timetableRepository.nullifyArtistId(artistId);
    }

    @Transactional
    @CacheEvict(value = "timetable", key = "#festivalId")
    public void deleteEntry(Long festivalId, Long entryId) {
        TimetableEntry entry = EntityLoader.getOrThrow(timetableRepository::findById, entryId, "타임테이블 항목");
        if (!festivalId.equals(entry.getFestivalId())) {
            throw new IllegalArgumentException("해당 페스티벌의 항목이 아닙니다.");
        }
        timetableRepository.delete(entry);
    }
}
