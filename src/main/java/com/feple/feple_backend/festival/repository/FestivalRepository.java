package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.global.MusicGenre;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    // ── 소프트 삭제 ──────────────────────────────────────────────────────────
    Page<Festival> findAllByDeletedAtIsNull(Pageable pageable);
    Optional<Festival> findByIdAndDeletedAtIsNull(Long id);
    long countByDeletedAtIsNull();

    @Query(value = "SELECT * FROM festival WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", nativeQuery = true)
    List<Festival> findSoftDeleted();

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE festival SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restoreById(@Param("id") Long id);

    // ── 좋아요 카운터 (원자적 증감 — race condition 방지) ─────────────────────
    @Modifying
    @Transactional
    @Query("UPDATE Festival f SET f.likeCount = f.likeCount + 1 WHERE f.id = :id")
    void incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE festival SET like_count = GREATEST(like_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementLikeCount(@Param("id") Long id);

    // ── 참석 카운터 (원자적 증감 — race condition 방지) ─────────────────────
    @Modifying
    @Transactional
    @Query("UPDATE Festival f SET f.attendingCount = f.attendingCount + 1 WHERE f.id = :id")
    void incrementAttendingCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE festival SET attending_count = GREATEST(attending_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementAttendingCount(@Param("id") Long id);

    @Query("SELECT f FROM Festival f WHERE f.startDate = :startDate AND f.deletedAt IS NULL")
    List<Festival> findByStartDate(@Param("startDate") LocalDate startDate);

    // 진행 중이거나 N일 이내 시작하는 페스티벌 (날씨 수집 대상)
    @Query("SELECT f FROM Festival f WHERE f.deletedAt IS NULL AND f.startDate <= :before AND (f.endDate IS NULL OR f.endDate >= :today)")
    List<Festival> findOngoingOrStartingBefore(@Param("today") LocalDate today, @Param("before") LocalDate before);

    // FULLTEXT ngram 매치 — LIKE '%keyword%'는 B-tree 인덱스를 못 타 풀스캔이었음.
    // REPLACE로 큰따옴표를 제거해 boolean 모드 phrase 구문이 깨지지 않게 방어한다.
    @Query(value = "SELECT * FROM festival WHERE deleted_at IS NULL AND MATCH(title) AGAINST (CONCAT('\"', REPLACE(:keyword, '\"', ''), '\"') IN BOOLEAN MODE) LIMIT :limit",
           nativeQuery = true)
    List<Festival> findByTitleKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    @Query("SELECT f FROM Festival f WHERE f.deletedAt IS NULL AND LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' ORDER BY f.startDate DESC")
    org.springframework.data.domain.Page<Festival> findByTitleKeywordPaged(
            @Param("keyword") String keyword, Pageable pageable);

    // activeFrom: null이면 종료된 축제 포함, non-null이면 endDate >= activeFrom인 것만 반환.
    // 상태 분류·정렬은 서비스 레이어(FestivalStatus)가 담당해 여기선 최종 개수를 알 수 없으므로,
    // Pageable로 넉넉한 상한(PageSize.FESTIVAL_FILTER_FETCH_CAP)만 적용해 무제한 전체조회를 막는다.
    @Query("SELECT DISTINCT f FROM Festival f LEFT JOIN f.genres g " +
           "WHERE f.deletedAt IS NULL " +
           "AND (:genres IS NULL OR g IN :genres) " +
           "AND (:regions IS NULL OR f.region IN :regions) " +
           "AND (:ageRestrictions IS NULL OR f.ageRestriction IN :ageRestrictions) " +
           "AND (:activeFrom IS NULL OR f.endDate IS NULL OR f.endDate >= :activeFrom) " +
           "ORDER BY f.startDate DESC")
    List<Festival> findByFilters(@Param("genres") List<MusicGenre> genres,
                                 @Param("regions") List<Region> regions,
                                 @Param("ageRestrictions") List<AgeRestriction> ageRestrictions,
                                 @Param("activeFrom") LocalDate activeFrom,
                                 Pageable pageable);

    // 프론트 검색/브라우즈 화면 전용 — 정렬은 호출부가 Pageable에 담아 넘긴 Sort를 그대로 사용한다.
    // DISTINCT+JOIN 쿼리는 count 쿼리를 자동 유추하면 부정확할 수 있어 명시한다.
    @Query(value = "SELECT DISTINCT f FROM Festival f LEFT JOIN f.genres g " +
           "WHERE f.deletedAt IS NULL " +
           "AND (:genres IS NULL OR g IN :genres) " +
           "AND (:regions IS NULL OR f.region IN :regions) " +
           "AND (:ageRestrictions IS NULL OR f.ageRestriction IN :ageRestrictions) " +
           "AND (:activeFrom IS NULL OR f.endDate IS NULL OR f.endDate >= :activeFrom)",
           countQuery = "SELECT COUNT(DISTINCT f) FROM Festival f LEFT JOIN f.genres g " +
           "WHERE f.deletedAt IS NULL " +
           "AND (:genres IS NULL OR g IN :genres) " +
           "AND (:regions IS NULL OR f.region IN :regions) " +
           "AND (:ageRestrictions IS NULL OR f.ageRestriction IN :ageRestrictions) " +
           "AND (:activeFrom IS NULL OR f.endDate IS NULL OR f.endDate >= :activeFrom)")
    Page<Festival> findByFiltersPage(@Param("genres") List<MusicGenre> genres,
                                     @Param("regions") List<Region> regions,
                                     @Param("ageRestrictions") List<AgeRestriction> ageRestrictions,
                                     @Param("activeFrom") LocalDate activeFrom,
                                     Pageable pageable);

    // 정렬 미지정 시 기본 노출 순서 — FestivalStatus(ACTIVE는 임박한 순, ENDED는 최근 종료 순)와
    // 동일한 규칙을 SQL로 재현한다. 단순히 startDate로만 정렬하면 오래전 종료된 축제가 앞쪽
    // 페이지를 차지해, 홈 화면 캐러셀처럼 앞쪽 페이지만 보여주는 화면에서 진행중/예정 축제가
    // 전혀 안 보이는 문제가 있었다.
    @Query(value = "SELECT DISTINCT f FROM Festival f LEFT JOIN f.genres g " +
           "WHERE f.deletedAt IS NULL " +
           "AND (:genres IS NULL OR g IN :genres) " +
           "AND (:regions IS NULL OR f.region IN :regions) " +
           "AND (:ageRestrictions IS NULL OR f.ageRestriction IN :ageRestrictions) " +
           "AND (:activeFrom IS NULL OR f.endDate IS NULL OR f.endDate >= :activeFrom) " +
           "ORDER BY " +
           "CASE WHEN f.endDate IS NULL OR f.endDate >= :today THEN 0 ELSE 1 END, " +
           "CASE WHEN f.endDate IS NULL OR f.endDate >= :today THEN f.startDate END ASC, " +
           "CASE WHEN f.endDate IS NOT NULL AND f.endDate < :today THEN f.startDate END DESC",
           countQuery = "SELECT COUNT(DISTINCT f) FROM Festival f LEFT JOIN f.genres g " +
           "WHERE f.deletedAt IS NULL " +
           "AND (:genres IS NULL OR g IN :genres) " +
           "AND (:regions IS NULL OR f.region IN :regions) " +
           "AND (:ageRestrictions IS NULL OR f.ageRestriction IN :ageRestrictions) " +
           "AND (:activeFrom IS NULL OR f.endDate IS NULL OR f.endDate >= :activeFrom)")
    Page<Festival> findByFiltersPageDefaultOrder(@Param("genres") List<MusicGenre> genres,
                                     @Param("regions") List<Region> regions,
                                     @Param("ageRestrictions") List<AgeRestriction> ageRestrictions,
                                     @Param("activeFrom") LocalDate activeFrom,
                                     @Param("today") LocalDate today,
                                     Pageable pageable);

    List<Festival> findTop10ByDeletedAtIsNullOrderByLikeCountDesc();

    @Query("SELECT f FROM Festival f WHERE f.deletedAt IS NULL AND f.startDate BETWEEN :today AND :until ORDER BY f.likeCount DESC")
    List<Festival> findUpcomingFestivalsSortedByLike(
            @Param("today") LocalDate today,
            @Param("until") LocalDate until,
            Pageable pageable);

}
