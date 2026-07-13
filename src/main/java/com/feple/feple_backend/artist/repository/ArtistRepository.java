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

    // FULLTEXT 검색: artist.name/name_en + artist_aliases.alias 각각 인덱스 사용
    @Query(value = "SELECT DISTINCT a.* FROM artist a " +
                   "LEFT JOIN artist_aliases aa ON aa.artist_id = a.id " +
                   "WHERE MATCH(a.name, a.name_en) AGAINST (CONCAT('\"', REPLACE(:keyword, '\"', ''), '\"') IN BOOLEAN MODE) " +
                   "   OR MATCH(aa.alias) AGAINST (CONCAT('\"', REPLACE(:keyword, '\"', ''), '\"') IN BOOLEAN MODE) " +
                   "ORDER BY a.name ASC " +
                   "LIMIT :limit",
           nativeQuery = true)
    java.util.List<Artist> searchArtistsByNameFullText(@Param("keyword") String keyword, @Param("limit") int limit);

    // LIKE fallback (관리자 목록 검색·OCR 자동매칭 — 정확한 부분일치 필요)
    @Query("SELECT DISTINCT a FROM Artist a LEFT JOIN a.aliases alias " +
           "WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "OR LOWER(alias) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "ORDER BY a.name ASC")
    java.util.List<Artist> findByNameOrNameEnContainingIgnoreCase(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT a FROM Artist a LEFT JOIN a.aliases alias " +
           "WHERE LOWER(a.name) = LOWER(:name) " +
           "OR LOWER(a.nameEn) = LOWER(:name) " +
           "OR LOWER(alias) = LOWER(:name)")
    java.util.Optional<Artist> findExactByNameIgnoreCase(@Param("name") String name);

    java.util.List<Artist> findTop10ByOrderByFollowerCountDesc();

    @Query("SELECT a.name FROM Artist a WHERE a.name IS NOT NULL AND a.name <> ''")
    java.util.List<String> findAllKoreanNames();

    @Query("SELECT a.nameEn FROM Artist a WHERE a.nameEn IS NOT NULL AND a.nameEn <> ''")
    java.util.List<String> findAllEnglishNames();

}
