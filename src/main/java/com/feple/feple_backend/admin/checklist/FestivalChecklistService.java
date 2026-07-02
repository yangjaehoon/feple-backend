package com.feple.feple_backend.admin.checklist;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(value = "festivalChecklistMap", key = "'all'")
    public Map<Long, FestivalChecklist> getChecklistMap(List<Long> festivalIds) {
        return checklistRepository.findByFestivalIdIn(festivalIds)
                .stream()
                .collect(Collectors.toMap(FestivalChecklist::getFestivalId, c -> c));
    }

    @Transactional
    @CacheEvict(value = "festivalChecklistMap", allEntries = true)
    public void toggle(Long festivalId, String field) {
        FestivalChecklist checklist = checklistRepository.findByFestivalId(festivalId)
                .orElseGet(() -> checklistRepository.save(FestivalChecklist.of(festivalId)));
        checklist.toggle(field);
    }

    @Transactional(readOnly = true)
    public boolean isChecked(Long festivalId, String field) {
        return checklistRepository.findByFestivalId(festivalId)
                .map(c -> c.valueOf(field))
                .orElse(false);
    }

    @Transactional
    @CacheEvict(value = "festivalChecklistMap", allEntries = true)
    public void saveMemo(Long festivalId, String memo) {
        FestivalChecklist checklist = checklistRepository.findByFestivalId(festivalId)
                .orElseGet(() -> checklistRepository.save(FestivalChecklist.of(festivalId)));
        checklist.updateMemo(memo);
    }
}
