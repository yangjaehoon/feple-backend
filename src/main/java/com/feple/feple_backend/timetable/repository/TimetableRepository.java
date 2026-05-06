package com.feple.feple_backend.timetable.repository;

import com.feple.feple_backend.timetable.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimetableRepository extends JpaRepository<TimetableEntry, Long> {
    // stage JOIN FETCH — getStageName()/getDisplayOrder() 접근 시 N+1 방지
    @Query("SELECT t FROM TimetableEntry t LEFT JOIN FETCH t.stage WHERE t.festival.id = :festivalId ORDER BY t.festivalDate ASC, t.startTime ASC")
    List<TimetableEntry> findByFestivalIdWithStage(@Param("festivalId") Long festivalId);
    List<TimetableEntry> findByFestivalIdAndArtistName(Long festivalId, String artistName);

    @Modifying
    @Query("DELETE FROM TimetableEntry t WHERE t.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);
}
