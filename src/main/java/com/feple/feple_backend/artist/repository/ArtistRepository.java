package com.feple.feple_backend.artist.repository;

import com.feple.feple_backend.artist.entity.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    @Query("SELECT a.followerCount FROM Artist a WHERE a.id = :artistId")
    int findFollowerCountById(@Param("artistId") Long artistId);

    @Query("SELECT a FROM Artist a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' ORDER BY a.name ASC")
    java.util.List<Artist> findByNameOrNameEnContainingIgnoreCase(@Param("keyword") String keyword);

    java.util.List<Artist> findAllByOrderByWeeklyScoreDescIdAsc();

    java.util.List<Artist> findTop10ByOrderByFollowerCountDesc();

    @Query("SELECT a.name FROM Artist a WHERE a.name IS NOT NULL AND a.name <> ''")
    java.util.List<String> findAllKoreanNames();

    @Query("SELECT a.nameEn FROM Artist a WHERE a.nameEn IS NOT NULL AND a.nameEn <> ''")
    java.util.List<String> findAllEnglishNames();

}
