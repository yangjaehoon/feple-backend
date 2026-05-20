package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistPhotoReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArtistPhotoReportRepository extends JpaRepository<ArtistPhotoReport, Long> {

    @Query("SELECT CASE WHEN COUNT(apr) > 0 THEN TRUE ELSE FALSE END FROM ArtistPhotoReport apr WHERE apr.reporter.id = :reporterId AND apr.photo.id = :photoId")
    boolean existsByReporterIdAndPhotoId(@Param("reporterId") Long reporterId, @Param("photoId") Long photoId);

    Page<ArtistPhotoReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Page<ArtistPhotoReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);

    @Query("SELECT apr FROM ArtistPhotoReport apr WHERE apr.photo.id = :photoId")
    List<ArtistPhotoReport> findByPhotoId(@Param("photoId") Long photoId);
}
