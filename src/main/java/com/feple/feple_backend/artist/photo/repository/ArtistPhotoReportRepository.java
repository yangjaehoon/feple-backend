package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistPhotoReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ArtistPhotoReportRepository extends JpaRepository<ArtistPhotoReport, Long> {

    @Query("SELECT CASE WHEN COUNT(apr) > 0 THEN TRUE ELSE FALSE END FROM ArtistPhotoReport apr WHERE apr.reporter.id = :reporterId AND apr.photo.id = :photoId")
    boolean existsByReporterIdAndPhotoId(@Param("reporterId") Long reporterId, @Param("photoId") Long photoId);

    @EntityGraph(attributePaths = {"photo", "photo.artist", "photo.uploader", "reporter"})
    Page<ArtistPhotoReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"photo", "photo.artist", "photo.uploader", "reporter"})
    Page<ArtistPhotoReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"photo", "photo.artist", "photo.uploader", "reporter"})
    @Query("SELECT apr FROM ArtistPhotoReport apr WHERE " +
           "(:status IS NULL OR apr.status = :status) AND " +
           "(LOWER(apr.photo.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(apr.photo.artist.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(apr.reporter.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY apr.createdAt DESC")
    Page<ArtistPhotoReport> searchByKeyword(@Param("keyword") String keyword,
                                             @Param("status") ReportStatus status,
                                             Pageable pageable);

    long countByStatus(ReportStatus status);

    @Query("SELECT apr FROM ArtistPhotoReport apr WHERE apr.photo.id = :photoId")
    List<ArtistPhotoReport> findByPhotoId(@Param("photoId") Long photoId);

    @Query("SELECT apr.photo.uploader.id, COUNT(apr) FROM ArtistPhotoReport apr WHERE apr.photo.uploader.id IN :userIds GROUP BY apr.photo.uploader.id")
    List<Object[]> countByPhotoUploaderIds(@Param("userIds") Collection<Long> userIds);
}
