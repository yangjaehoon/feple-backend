package com.feple.feple_backend.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FestivalChecklistRepository extends JpaRepository<FestivalChecklist, Long> {
    Optional<FestivalChecklist> findByFestivalId(Long festivalId);
    List<FestivalChecklist> findByFestivalIdIn(List<Long> festivalIds);

    @Modifying
    @Query("DELETE FROM FestivalChecklist fc WHERE fc.festivalId = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);
}
