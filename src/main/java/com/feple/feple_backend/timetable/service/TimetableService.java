package com.feple.feple_backend.timetable.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequest;
import com.feple.feple_backend.timetable.dto.TimetableEntryResponse;
import com.feple.feple_backend.timetable.entity.TimetableEntry;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final FestivalRepository festivalRepository;

    public List<TimetableEntryResponse> getEntries(Long festivalId) {
        return timetableRepository
                .findByFestivalIdOrderByFestivalDateAscStartTimeAsc(festivalId)
                .stream()
                .map(TimetableEntryResponse::from)
                .toList();
    }

    @Transactional
    public TimetableEntryResponse createEntry(Long festivalId, TimetableEntryRequest req) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new IllegalArgumentException("페스티벌을 찾을 수 없습니다."));
        TimetableEntry entry = TimetableEntry.builder()
                .festival(festival)
                .stageName(req.getStageName().trim())
                .artistName(req.getArtistName().trim())
                .festivalDate(req.getFestivalDate())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .build();
        return TimetableEntryResponse.from(timetableRepository.save(entry));
    }

    @Transactional
    public void deleteEntry(Long id) {
        timetableRepository.deleteById(id);
    }
}
