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

    @Query(value = "SELECT * FROM artist WHERE genre LIKE CONCAT('%', :genreName, '%')",
           countQuery = "SELECT COUNT(*) FROM artist WHERE genre LIKE CONCAT('%', :genreName, '%')",
           nativeQuery = true)
    Page<Artist> findByGenreName(@Param("genreName") String genreName, Pageable pageable);

    @Query("SELECT a FROM Artist a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' ORDER BY a.name ASC")
    java.util.List<Artist> findByNameOrNameEnContainingIgnoreCase(@Param("keyword") String keyword);

    @Query("SELECT a FROM Artist a WHERE LOWER(a.name) = LOWER(:name) OR LOWER(a.nameEn) = LOWER(:name)")
    java.util.Optional<Artist> findExactByNameIgnoreCase(@Param("name") String name);

    java.util.List<Artist> findTop10ByOrderByFollowerCountDesc();

    @Query("SELECT a.name FROM Artist a WHERE a.name IS NOT NULL AND a.name <> ''")
    java.util.List<String> findAllKoreanNames();

    @Query("SELECT a.nameEn FROM Artist a WHERE a.nameEn IS NOT NULL AND a.nameEn <> ''")
    java.util.List<String> findAllEnglishNames();

}
