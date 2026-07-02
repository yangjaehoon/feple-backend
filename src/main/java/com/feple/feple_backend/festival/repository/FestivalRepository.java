package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    // ── 좋아요 카운터 (원자적 증감 — race condition 방지) ─────────────────────
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Festival f SET f.likeCount = f.likeCount + 1 WHERE f.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE festival SET like_count = GREATEST(like_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementLikeCount(@Param("id") Long id);

    List<Festival> findAllByOrderByStartDateDesc();

    List<Festival> findByStartDate(LocalDate startDate);

    // 진행 중이거나 N일 이내 시작하는 페스티벌 (날씨 수집 대상)
    @Query("SELECT f FROM Festival f WHERE f.startDate <= :before AND (f.endDate IS NULL OR f.endDate >= :today)")
    List<Festival> findOngoingOrStartingBefore(@Param("today") LocalDate today, @Param("before") LocalDate before);

    @Query("SELECT f FROM Festival f WHERE LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!'")
    List<Festival> findByTitleKeyword(@Param("keyword") String keyword);

    @Query("SELECT f FROM Festival f WHERE LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' ORDER BY f.startDate DESC")
    org.springframework.data.domain.Page<Festival> findByTitleKeywordPaged(
            @Param("keyword") String keyword, Pageable pageable);

    // activeFrom: null이면 종료된 축제 포함, non-null이면 endDate >= activeFrom인 것만 반환
    @Query("SELECT DISTINCT f FROM Festival f LEFT JOIN f.genres g " +
           "WHERE (:genres IS NULL OR g IN :genres) " +
           "AND (:regions IS NULL OR f.region IN :regions) " +
           "AND (:ageRestrictions IS NULL OR f.ageRestriction IN :ageRestrictions) " +
           "AND (:activeFrom IS NULL OR f.endDate IS NULL OR f.endDate >= :activeFrom)")
    List<Festival> findByFilters(@Param("genres") List<Genre> genres,
                                 @Param("regions") List<Region> regions,
                                 @Param("ageRestrictions") List<AgeRestriction> ageRestrictions,
                                 @Param("activeFrom") LocalDate activeFrom);

    List<Festival> findTop10ByOrderByLikeCountDesc();

    @Query("SELECT f FROM Festival f WHERE f.startDate BETWEEN :today AND :until ORDER BY f.likeCount DESC")
    List<Festival> findUpcomingFestivalsSortedByLike(
            @Param("today") LocalDate today,
            @Param("until") LocalDate until,
            Pageable pageable);

    @Query("SELECT COUNT(f) FROM Festival f WHERE f.endDate IS NULL OR f.endDate >= :today")
    long countActiveFestivals(@Param("today") LocalDate today);

}
