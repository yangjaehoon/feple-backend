package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    List<Festival> findAllByOrderByStartDateDesc();

    List<Festival> findByStartDate(LocalDate startDate);

    // 진행 중이거나 N일 이내 시작하는 페스티벌 (날씨 수집 대상)
    @Query("SELECT f FROM Festival f WHERE f.startDate <= :before AND (f.endDate IS NULL OR f.endDate >= :today)")
    List<Festival> findOngoingOrStartingBefore(@Param("today") LocalDate today, @Param("before") LocalDate before);

    @Query("SELECT f FROM Festival f WHERE LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Festival> findByTitleKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT f FROM Festival f LEFT JOIN f.genres g " +
           "WHERE (:genres IS NULL OR g IN :genres) " +
           "AND (:regions IS NULL OR f.region IN :regions)")
    List<Festival> findByFilters(@Param("genres") List<Genre> genres,
                                 @Param("regions") List<Region> regions);

    @Modifying
    @Query("UPDATE Festival f SET f.likeCount = f.likeCount + 1 WHERE f.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Festival f SET f.likeCount = f.likeCount - 1 WHERE f.id = :id AND f.likeCount > 0")
    void decrementLikeCount(@Param("id") Long id);

}
