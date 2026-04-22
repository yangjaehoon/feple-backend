package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalStatus;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FestivalRepository extends JpaRepository<Festival, Long> {

    List<Festival> findAllByOrderByStartDateDesc();

    List<Festival> findByStartDate(java.time.LocalDate startDate);

    @Query("SELECT f FROM Festival f WHERE LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Festival> findByTitleKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT f FROM Festival f LEFT JOIN f.genres g " +
           "WHERE (:genres IS NULL OR g IN :genres) " +
           "AND (:regions IS NULL OR f.region IN :regions)")
    List<Festival> findByFilters(@Param("genres") List<Genre> genres,
                                 @Param("regions") List<Region> regions);

    List<Festival> findByStatusOrderByIdDesc(FestivalStatus status);

    boolean existsBySourceUrl(String sourceUrl);
}
