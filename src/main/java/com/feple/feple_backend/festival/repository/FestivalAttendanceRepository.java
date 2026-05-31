package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.entity.FestivalAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FestivalAttendanceRepository extends JpaRepository<FestivalAttendance, Long> {

    @Query("SELECT CASE WHEN COUNT(fa) > 0 THEN TRUE ELSE FALSE END FROM FestivalAttendance fa WHERE fa.user.id = :userId AND fa.festival.id = :festivalId")
    boolean existsByUserIdAndFestivalId(@Param("userId") Long userId, @Param("festivalId") Long festivalId);

    @Modifying
    @Query("DELETE FROM FestivalAttendance fa WHERE fa.user.id = :userId AND fa.festival.id = :festivalId")
    int deleteByUserIdAndFestivalId(@Param("userId") Long userId, @Param("festivalId") Long festivalId);

    @Modifying
    @Query("DELETE FROM FestivalAttendance fa WHERE fa.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);
}
