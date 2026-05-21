package com.feple.feple_backend.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FestivalChecklistRepository extends JpaRepository<FestivalChecklist, Long> {
    Optional<FestivalChecklist> findByFestivalId(Long festivalId);
    List<FestivalChecklist> findByFestivalIdIn(List<Long> festivalIds);
}
