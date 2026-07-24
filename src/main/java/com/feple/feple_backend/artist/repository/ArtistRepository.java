package com.feple.feple_backend.artist.repository;

import com.feple.feple_backend.artist.entity.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    // ── 팔로워 카운터 (원자적 증감 — race condition 방지) ────────────────────
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Artist a SET a.followerCount = a.followerCount + 1 WHERE a.id = :id")
    void incrementFollowerCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE artist SET follower_count = GREATEST(follower_count - 1, 0) WHERE id = :id", nativeQuery = true)
    void decrementFollowerCount(@Param("id") Long id);

    @Query(value = "SELECT DISTINCT a.* FROM artist a JOIN artist_genres ag ON ag.artist_id = a.id WHERE ag.genres = :genreName",
           countQuery = "SELECT COUNT(DISTINCT a.id) FROM artist a JOIN artist_genres ag ON ag.artist_id = a.id WHERE ag.genres = :genreName",
           nativeQuery = true)
    Page<Artist> findByGenreName(@Param("genreName") String genreName, Pageable pageable);

    // 아티스트 이름/영문명/별명 검색 (일반 검색 + 관리자 목록 검색 + OCR 자동매칭)
    @Query("SELECT DISTINCT a FROM Artist a LEFT JOIN a.aliases alias " +
           "WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "OR LOWER(alias) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "ORDER BY a.name ASC")
    java.util.List<Artist> findByNameOrNameEnContainingIgnoreCase(@Param("keyword") String keyword);

    // 라인업 OCR 자동매칭 배치 조회용 — 이름 하나마다 개별 쿼리하는 N+1을 피하기 위해
    // 전체 아티스트+alias를 한 번에 가져와 메모리에서 매칭한다 (ArtistLineupOcrService).
    @Query("SELECT DISTINCT a FROM Artist a LEFT JOIN FETCH a.aliases")
    java.util.List<Artist> findAllWithAliases();

    java.util.List<Artist> findTop10ByOrderByFollowerCountDesc();

    @Query("SELECT a.name FROM Artist a WHERE a.name IS NOT NULL AND a.name <> ''")
    java.util.List<String> findAllKoreanNames();

    @Query("SELECT a.nameEn FROM Artist a WHERE a.nameEn IS NOT NULL AND a.nameEn <> ''")
    java.util.List<String> findAllEnglishNames();

}
