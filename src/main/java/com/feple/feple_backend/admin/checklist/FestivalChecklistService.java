package com.feple.feple_backend.admin.checklist;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * @return 토글 후 값 — 컨트롤러가 JSON 응답과 감사 로그에 사용. 별도 조회로 분리하면 그 사이에
     * 다른 요청이 끼어들어 감사 로그에 실제와 다른 값이 남을 수 있어, 이미 메모리에 로드된 엔티티의
     * 값을 그대로 반환한다(추가 쿼리 없음).
     */
    @Transactional
    @CacheEvict(value = "festivalChecklistMap", allEntries = true)
    public boolean toggle(Long festivalId, String field) {
        FestivalChecklist checklist = getOrCreate(festivalId);
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
        FestivalChecklist checklist = getOrCreate(festivalId);
        checklist.updateMemo(memo);
    }

    // festival_id unique 제약 위반(같은 festival의 체크리스트를 처음 만드는 두 요청이 경합)이 나면
    // 상대가 이미 만든 행을 재조회해 이번 요청의 toggle/updateMemo가 유실되지 않게 한다.
    private FestivalChecklist getOrCreate(Long festivalId) {
        return checklistRepository.findByFestivalId(festivalId)
                .orElseGet(() -> {
                    try {
                        return checklistRepository.save(FestivalChecklist.of(festivalId));
                    } catch (DataIntegrityViolationException e) {
                        return checklistRepository.findByFestivalId(festivalId).orElseThrow(() -> e);
                    }
                });
    }

    /** 페스티벌 삭제 시 연관 체크리스트를 정리한다. Repository를 직접 호출하면 이 캐시가 무효화되지 않으므로 반드시 이 메서드를 거칠 것. */
    @Transactional
    @CacheEvict(value = "festivalChecklistMap", allEntries = true)
    public void removeByFestivalId(Long festivalId) {
        checklistRepository.deleteByFestivalId(festivalId);
    }
}
