package com.feple.feple_backend.artist.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.global.MusicGenre;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    // ── 소프트 삭제 ──────────────────────────────────────────────────────────
    java.util.List<Artist> findAllByDeletedAtIsNull(Sort sort);
    Page<Artist> findAllByDeletedAtIsNull(Pageable pageable);
    java.util.List<Artist> findAllByDeletedAtIsNull();
    Optional<Artist> findByIdAndDeletedAtIsNull(Long id);
    long countByDeletedAtIsNull();

    @Query(value = "SELECT * FROM artist WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC", nativeQuery = true)
    java.util.List<Artist> findSoftDeleted();

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE artist SET deleted_at = NULL WHERE id = :id", nativeQuery = true)
    void restoreById(@Param("id") Long id);

    // ── 팔로워 카운터 (원자적 증감 — race condition 방지) ────────────────────
    // clearAutomatically를 쓰지 않는다(영속성 컨텍스트 전체를 비워 다른 작업을 detach시키는 부작용).
    // 갱신 직후의 최신 값이 필요하면 findFollowerCountById로 스칼라 조회한다.
    @Modifying
    @Transactional
    @Query("UPDATE Artist a SET a.followerCount = a.followerCount + 1 WHERE a.id = :id")
    void incrementFollowerCount(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE artist SET follower_count = GREATEST(follower_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementFollowerCount(@Param("id") Long id);

    /** 카운터 증감 직후 최신 팔로워 수만 스칼라로 다시 읽는다 (엔티티 재조회·컨텍스트 clear 불필요). */
    @Query("SELECT a.followerCount FROM Artist a WHERE a.id = :id")
    Integer findFollowerCountById(@Param("id") Long id);

    // 네이티브 쿼리로 두면 Pageable의 Sort 속성명(followerCount, weeklyScore)이 그대로
    // ORDER BY 컬럼명으로 붙어 "Unknown column" 에러가 난다 (실제 컬럼은 snake_case).
    // JPQL로 두면 Sort가 엔티티 속성명으로 해석되므로 관리자 목록의 모든 정렬 옵션과 호환된다.
    @Query("SELECT DISTINCT a FROM Artist a JOIN a.genres g WHERE g = :genre AND a.deletedAt IS NULL")
    Page<Artist> findByGenre(@Param("genre") MusicGenre genre, Pageable pageable);

    // 아티스트 이름/영문명/별명 검색 (일반 검색 + 관리자 목록 검색 + OCR 자동매칭)
    // 접두사 일치(예: "han" → "Han Yohan")를 중간 일치(예: "han" → "Nochang" 안의 "han")보다
    // 우선 정렬 — 안 그러면 관련 없는 중간 일치가 가나다순 정렬에서 앞서 나와, 검색/자동완성의
    // limit(MAX_RESULTS/MAX_SUGGESTIONS)에 정작 찾던 접두사 일치 결과가 잘려나갈 수 있다.
    @Query("SELECT DISTINCT a FROM Artist a LEFT JOIN a.aliases alias " +
           "WHERE a.deletedAt IS NULL AND (" +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "OR LOWER(alias) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
           "ORDER BY CASE WHEN LOWER(a.name) LIKE LOWER(CONCAT(:keyword, '%')) ESCAPE '!' " +
           "OR LOWER(a.nameEn) LIKE LOWER(CONCAT(:keyword, '%')) ESCAPE '!' " +
           "THEN 0 ELSE 1 END, a.name ASC")
    java.util.List<Artist> findByNameOrNameEnContainingIgnoreCase(@Param("keyword") String keyword);

    // 라인업 OCR 자동매칭 배치 조회용 — 이름 하나마다 개별 쿼리하는 N+1을 피하기 위해
    // 전체 아티스트+alias를 한 번에 가져와 메모리에서 매칭한다 (ArtistLineupOcrService).
    @Query("SELECT DISTINCT a FROM Artist a LEFT JOIN FETCH a.aliases WHERE a.deletedAt IS NULL")
    java.util.List<Artist> findAllWithAliases();

    java.util.List<Artist> findTop10ByDeletedAtIsNullOrderByFollowerCountDesc();

    @Query("SELECT a.name FROM Artist a WHERE a.deletedAt IS NULL AND a.name IS NOT NULL AND a.name <> ''")
    java.util.List<String> findAllKoreanNames();

    @Query("SELECT a.nameEn FROM Artist a WHERE a.deletedAt IS NULL AND a.nameEn IS NOT NULL AND a.nameEn <> ''")
    java.util.List<String> findAllEnglishNames();

}
