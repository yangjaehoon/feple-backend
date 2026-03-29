package com.feple.feple_backend.timetable.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final FestivalRepository festivalRepository;
    private final StageRepository stageRepository;

    public List<TimetableEntryResponse> getEntries(Long festivalId) {
        // 스테이지명 → displayOrder 매핑
        Map<String, Integer> stageOrderMap = stageRepository
                .findByFestivalIdOrderByDisplayOrder(festivalId)
                .stream()
                .collect(Collectors.toMap(s -> s.getName(), s -> s.getDisplayOrder(),
                        (a, b) -> a)); // 동명 스테이지는 첫 번째 값 사용

        return timetableRepository
                .findByFestivalIdOrderByFestivalDateAscStartTimeAsc(festivalId)
                .stream()
                .map(e -> {
                    int order = stageOrderMap.getOrDefault(e.getStageName(), Integer.MAX_VALUE);
                    return TimetableEntryResponse.from(e, order);
                })
                .sorted(Comparator
                        .comparing(TimetableEntryResponse::getFestivalDate)
                        .thenComparingInt(TimetableEntryResponse::getStageOrder)
                        .thenComparing(TimetableEntryResponse::getStartTime))
                .toList();
    }

    @Transactional
    public TimetableEntryResponse createEntry(Long festivalId, TimetableEntryRequest req) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new IllegalArgumentException("페스티벌을 찾을 수 없습니다."));
        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new IllegalArgumentException("종료 시간은 시작 시간보다 늦어야 합니다.");
        }
        TimetableEntry entry = TimetableEntry.builder()
                .festival(festival)
                .stageName(req.getStageName().trim())
                .artistName(req.getArtistName().trim())
                .festivalDate(req.getFestivalDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .build();
        TimetableEntry saved = timetableRepository.save(entry);
        int order = stageRepository.findByFestivalIdOrderByDisplayOrder(festivalId)
                .stream().filter(s -> s.getName().equals(saved.getStageName()))
                .mapToInt(s -> s.getDisplayOrder()).findFirst().orElse(Integer.MAX_VALUE);
        return TimetableEntryResponse.from(saved, order);
    }

    @Transactional
    public void deleteEntry(Long id) {
        timetableRepository.deleteById(id);
    }
}
