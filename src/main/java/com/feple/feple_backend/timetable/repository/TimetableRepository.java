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
    @Query("SELECT t FROM TimetableEntry t WHERE t.festival.id = :festivalId AND t.artistName = :artistName")
    List<TimetableEntry> findByFestivalIdAndArtistName(@Param("festivalId") Long festivalId, @Param("artistName") String artistName);

    @Modifying
    @Query("DELETE FROM TimetableEntry t WHERE t.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TimetableEntry t SET t.artist = null WHERE t.artist.id = :artistId")
    void nullifyArtistId(@Param("artistId") Long artistId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE TimetableEntry t SET t.stage = null WHERE t.stage.id = :stageId")
    void nullifyStageId(@Param("stageId") Long stageId);
}
