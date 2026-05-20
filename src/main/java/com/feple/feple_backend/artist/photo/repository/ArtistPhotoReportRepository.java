package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistPhotoReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtistPhotoReportRepository extends JpaRepository<ArtistPhotoReport, Long> {

    @Query("SELECT CASE WHEN COUNT(apr) > 0 THEN TRUE ELSE FALSE END FROM ArtistPhotoReport apr WHERE apr.reporter.id = :reporterId AND apr.photo.id = :photoId")
    boolean existsByReporterIdAndPhotoId(@Param("reporterId") Long reporterId, @Param("photoId") Long photoId);
}
