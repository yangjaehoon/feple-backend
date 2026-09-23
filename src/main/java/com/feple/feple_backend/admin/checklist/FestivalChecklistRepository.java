package com.feple.feple_backend.admin.checklist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalChecklistRepository extends JpaRepository<FestivalChecklist, Long> {
    Optional<FestivalChecklist> findByFestivalId(Long festivalId);
    List<FestivalChecklist> findByFestivalIdIn(List<Long> festivalIds);
}
