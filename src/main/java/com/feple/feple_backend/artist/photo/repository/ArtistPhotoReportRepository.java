package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistPhotoReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistPhotoReportRepository extends JpaRepository<ArtistPhotoReport, Long> {
    boolean existsByReporterIdAndPhotoId(Long reporterId, Long photoId);
}
