package com.feple.feple_backend.admin.checklist;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FestivalChecklistService {

    private final FestivalChecklistRepository checklistRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "festivalChecklistMap", key = "'all'")
    public Map<Long, FestivalChecklist> getChecklistMap() {
        return checklistRepository.findAll()
                .stream()
                .collect(Collectors.toMap(FestivalChecklist::getFestivalId, c -> c));
    }

    @Transactional
    @CacheEvict(value = "festivalChecklistMap", allEntries = true)
    public boolean toggle(Long festivalId, String field) {
        FestivalChecklist checklist = checklistRepository.findByFestivalId(festivalId)
                .orElseGet(() -> checklistRepository.save(FestivalChecklist.of(festivalId)));
        checklist.toggle(field);
        return checklist.isChecked(field);
    }

    @Transactional(readOnly = true)
    public boolean isChecked(Long festivalId, String field) {
        return checklistRepository.findByFestivalId(festivalId)
                .map(c -> c.isChecked(field))
                .orElse(false);
    }

    @Transactional
    @CacheEvict(value = "festivalChecklistMap", allEntries = true)
    public void saveMemo(Long festivalId, String memo) {
        FestivalChecklist checklist = checklistRepository.findByFestivalId(festivalId)
                .orElseGet(() -> checklistRepository.save(FestivalChecklist.of(festivalId)));
        checklist.updateMemo(memo);
    }

    /** 페스티벌 삭제 시 연관 체크리스트를 정리한다. Repository를 직접 호출하면 이 캐시가 무효화되지 않으므로 반드시 이 메서드를 거칠 것. */
    @Transactional
    @CacheEvict(value = "festivalChecklistMap", allEntries = true)
    public void removeByFestivalId(Long festivalId) {
        checklistRepository.deleteByFestivalId(festivalId);
    }
}
