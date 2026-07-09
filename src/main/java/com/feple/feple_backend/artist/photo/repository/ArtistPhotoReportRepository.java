package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistPhotoReport;
import com.feple.feple_backend.global.repository.BaseReportRepository;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ArtistPhotoReportRepository extends BaseReportRepository<ArtistPhotoReport> {

    @Query("SELECT CASE WHEN COUNT(apr) > 0 THEN TRUE ELSE FALSE END FROM ArtistPhotoReport apr WHERE apr.reporter.id = :reporterId AND apr.photo.id = :photoId")
    boolean existsByReporterIdAndPhotoId(@Param("reporterId") Long reporterId, @Param("photoId") Long photoId);

    @Override
    @EntityGraph(attributePaths = {"photo", "photo.artist", "photo.uploader", "reporter"})
    Page<ArtistPhotoReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"photo", "photo.artist", "photo.uploader", "reporter"})
    Page<ArtistPhotoReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Override
    @Query("SELECT DISTINCT apr FROM ArtistPhotoReport apr " +
           "JOIN FETCH apr.photo ph " +
           "JOIN FETCH ph.artist a " +
           "JOIN FETCH ph.uploader " +
           "JOIN FETCH apr.reporter " +
           "LEFT JOIN a.aliases alias " +
           "WHERE (:status IS NULL OR apr.status = :status) AND " +
           "(LOWER(ph.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           " LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           " LOWER(alias) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           " LOWER(apr.reporter.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
           "ORDER BY apr.createdAt DESC")
    Page<ArtistPhotoReport> searchByKeyword(@Param("keyword") String keyword,
                                             @Param("status") ReportStatus status,
                                             Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM ArtistPhotoReport apr WHERE apr.photo.id = :photoId")
    void deleteAllByPhotoId(@Param("photoId") Long photoId);

    @Query("SELECT apr.photo.uploader.id, COUNT(apr) FROM ArtistPhotoReport apr WHERE apr.photo.uploader.id IN :userIds GROUP BY apr.photo.uploader.id")
    List<Object[]> countByPhotoUploaderIds(@Param("userIds") Collection<Long> userIds);
}
