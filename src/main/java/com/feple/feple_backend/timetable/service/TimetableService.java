package com.feple.feple_backend.timetable.service;

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

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final FestivalRepository festivalRepository;
    private final StageRepository stageRepository;

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
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("페스티벌을 찾을 수 없습니다."));
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }
        Stage stage = stageRepository.findByFestivalIdAndName(festivalId, req.getStageName().trim())
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 스테이지입니다: " + req.getStageName()));

        TimetableEntry entry = TimetableEntry.builder()
                .festival(festival)
                .stage(stage)
                .artistName(req.getArtistName() != null ? req.getArtistName().trim() : "")
                .festivalDate(req.getFestivalDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .build();
        TimetableEntry saved = timetableRepository.save(entry);
        return TimetableEntryResponse.from(saved);
    }

    @Transactional
    public void deleteEntry(Long id) {
        timetableRepository.deleteById(id);
    }
}
