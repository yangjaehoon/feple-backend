package com.feple.feple_backend.stage.repository;

import com.feple.feple_backend.stage.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;


public interface StageRepository extends JpaRepository<Stage, Long> {

    @Query("SELECT s FROM Stage s WHERE s.festival.id = :festivalId ORDER BY s.displayOrder ASC")
    List<Stage> findByFestivalIdOrderByDisplayOrder(@Param("festivalId") Long festivalId);

    @Query("SELECT s FROM Stage s WHERE s.festival.id = :festivalId AND s.displayOrder < :displayOrder ORDER BY s.displayOrder DESC")
    Optional<Stage> findFirstByFestivalIdAndDisplayOrderLessThanOrderByDisplayOrderDesc(@Param("festivalId") Long festivalId, @Param("displayOrder") int displayOrder);

    @Query("SELECT s FROM Stage s WHERE s.festival.id = :festivalId AND s.displayOrder > :displayOrder ORDER BY s.displayOrder ASC")
    Optional<Stage> findFirstByFestivalIdAndDisplayOrderGreaterThanOrderByDisplayOrderAsc(@Param("festivalId") Long festivalId, @Param("displayOrder") int displayOrder);

    @Query("SELECT s FROM Stage s WHERE s.festival.id = :festivalId AND s.name = :name")
    Optional<Stage> findByFestivalIdAndName(@Param("festivalId") Long festivalId, @Param("name") String name);

    @Modifying
    @Transactional
    @Query("DELETE FROM Stage s WHERE s.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);

    @Query("SELECT COALESCE(MAX(s.displayOrder), 0) FROM Stage s WHERE s.festival.id = :festivalId")
    int findMaxDisplayOrderByFestivalId(@Param("festivalId") Long festivalId);
}
