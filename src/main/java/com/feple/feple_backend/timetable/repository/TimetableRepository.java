package com.feple.feple_backend.timetable.repository;

import com.feple.feple_backend.timetable.entity.TimetableEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TimetableRepository extends JpaRepository<TimetableEntry, Long> {
    // stage + artist + members + member.artist JOIN FETCH — N+1 방지
    @Query("SELECT DISTINCT t FROM TimetableEntry t LEFT JOIN FETCH t.stage LEFT JOIN FETCH t.artist LEFT JOIN FETCH t.members m LEFT JOIN FETCH m.artist WHERE t.festival.id = :festivalId ORDER BY t.festivalDate ASC, t.startTime ASC")
    List<TimetableEntry> findByFestivalIdWithStage(@Param("festivalId") Long festivalId);
    @Query("SELECT t FROM TimetableEntry t WHERE t.festival.id = :festivalId AND t.artistName = :artistName")
    List<TimetableEntry> findByFestivalIdAndArtistName(@Param("festivalId") Long festivalId, @Param("artistName") String artistName);

    // artist JOIN FETCH — getArtistName()에서 artist FK 접근 시 N+1 방지
    @Query("SELECT DISTINCT t FROM TimetableEntry t LEFT JOIN FETCH t.artist WHERE t.festival.id IN :festivalIds")
    List<TimetableEntry> findByFestivalIdInWithArtist(@Param("festivalIds") List<Long> festivalIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM TimetableEntry t WHERE t.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);

    // 스테이지 삭제 시: FK를 끊기 전에 삭제될 스테이지 이름을 stage_name에 스냅샷해
    // 해당 항목들이 무대 라벨을 잃지 않도록 한다 (FK 없는 항목의 폴백 문자열).
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE TimetableEntry t SET t.stageName = :stageName, t.stage = null WHERE t.stage.id = :stageId")
    void detachStage(@Param("stageId") Long stageId, @Param("stageName") String stageName);
}
