package com.feple.feple_backend.admin.checklist;

import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.EntityLoader;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalChecklistService {

    private final FestivalChecklistRepository checklistRepository;
    private final FestivalRepository festivalRepository;

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

    // 존재하지 않는 festivalId면 INSERT가 FK(fk_fc_festival)에서 터져 500이 되므로 먼저 확인한다.
    //
    // 같은 festival의 체크리스트를 처음 만드는 두 요청이 경합하면 unique(uq_fc_festival_id) 위반이
    // 날 수 있는데, 이때 예외를 catch해 재조회해도 소용이 없다 — save()는 자체 @Transactional을 가진
    // 프록시 호출이라 예외가 프록시 밖으로 나온 시점에 이미 트랜잭션이 rollback-only로 마킹되고,
    // 이어지는 작업은 커밋 시점에 UnexpectedRollbackException으로 통째로 롤백된다. 복구되는 척하지
    // 않고 그대로 실패시킨다(관리자 단건 액션이라 재시도하면 상대가 만든 행을 찾아 정상 처리된다).
    private FestivalChecklist getOrCreate(Long festivalId) {
        return checklistRepository.findByFestivalId(festivalId)
                .orElseGet(() -> {
                    EntityLoader.requireExists(festivalRepository::existsById, festivalId, "페스티벌");
                    return checklistRepository.save(FestivalChecklist.of(festivalId));
                });
    }
}
