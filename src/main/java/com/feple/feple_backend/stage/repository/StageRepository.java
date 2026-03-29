package com.feple.feple_backend.stage.repository;

import com.feple.feple_backend.stage.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findByFestivalIdOrderByDisplayOrder(Long festivalId);
    Optional<Stage> findFirstByFestivalIdAndDisplayOrderLessThanOrderByDisplayOrderDesc(Long festivalId, int displayOrder);
    Optional<Stage> findFirstByFestivalIdAndDisplayOrderGreaterThanOrderByDisplayOrderAsc(Long festivalId, int displayOrder);
    int countByFestivalId(Long festivalId);
    void deleteByFestivalId(Long festivalId);
}
