package com.feple.feple_backend.artist.repository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Page<Artist> findByGenre(ArtistGenre genre, Pageable pageable);

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
