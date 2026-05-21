package com.feple.feple_backend.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FestivalChecklistService {

    private final FestivalChecklistRepository checklistRepository;

    @Transactional(readOnly = true)
    public Map<Long, FestivalChecklist> getChecklistMap(List<Long> festivalIds) {
        return checklistRepository.findByFestivalIdIn(festivalIds)
                .stream()
                .collect(Collectors.toMap(FestivalChecklist::getFestivalId, c -> c));
    }

    @Transactional
    public boolean toggle(Long festivalId, String field) {
        FestivalChecklist checklist = checklistRepository.findByFestivalId(festivalId)
                .orElseGet(() -> checklistRepository.save(FestivalChecklist.of(festivalId)));
        checklist.toggle(field);
        return checklist.valueOf(field);
    }

    @Transactional
    public void saveMemo(Long festivalId, String memo) {
        FestivalChecklist checklist = checklistRepository.findByFestivalId(festivalId)
                .orElseGet(() -> checklistRepository.save(FestivalChecklist.of(festivalId)));
        checklist.updateMemo(memo);
    }
}
