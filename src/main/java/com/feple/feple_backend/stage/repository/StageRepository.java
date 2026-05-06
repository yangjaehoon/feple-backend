package com.feple.feple_backend.stage.repository;

import com.feple.feple_backend.stage.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface StageRepository extends JpaRepository<Stage, Long> {
    List<Stage> findByFestivalIdOrderByDisplayOrder(Long festivalId);
    Optional<Stage> findFirstByFestivalIdAndDisplayOrderLessThanOrderByDisplayOrderDesc(Long festivalId, int displayOrder);
    Optional<Stage> findFirstByFestivalIdAndDisplayOrderGreaterThanOrderByDisplayOrderAsc(Long festivalId, int displayOrder);
    Optional<Stage> findByFestivalIdAndName(Long festivalId, String name);

    int countByFestivalId(Long festivalId);
    void deleteByFestivalId(Long festivalId);

    @Query("SELECT COALESCE(MAX(s.displayOrder), 0) FROM Stage s WHERE s.festival.id = :festivalId")
    int findMaxDisplayOrderByFestivalId(@Param("festivalId") Long festivalId);
}
