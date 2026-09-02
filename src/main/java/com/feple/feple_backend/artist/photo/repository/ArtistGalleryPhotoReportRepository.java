package com.feple.feple_backend.artist.photo.repository;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoReport;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.repository.BaseReportRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtistGalleryPhotoReportRepository extends BaseReportRepository<ArtistGalleryPhotoReport> {

    @Query("SELECT CASE WHEN COUNT(apr) > 0 THEN TRUE ELSE FALSE END FROM ArtistGalleryPhotoReport apr WHERE apr.reporter.id = :reporterId AND apr.photo.id = :photoId")
    boolean existsByReporterIdAndPhotoId(@Param("reporterId") Long reporterId, @Param("photoId") Long photoId);

    @Override
    @EntityGraph(attributePaths = {"photo", "photo.artist", "photo.uploader", "reporter"})
    Page<ArtistGalleryPhotoReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"photo", "photo.artist", "photo.uploader", "reporter"})
    Page<ArtistGalleryPhotoReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Override
    @Query("SELECT DISTINCT apr FROM ArtistGalleryPhotoReport apr " +
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
    Page<ArtistGalleryPhotoReport> searchByKeyword(@Param("keyword") String keyword,
                                                   @Param("status") ReportStatus status,
                                                   Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM ArtistGalleryPhotoReport apr WHERE apr.photo.id = :photoId")
    void deleteAllByPhotoId(@Param("photoId") Long photoId);

    // 회원 완전 삭제(hardDelete) 시 users 행 물리 삭제 전에 이 유저가 낸 신고를 비운다 (reporter_id FK RESTRICT).
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM ArtistGalleryPhotoReport apr WHERE apr.reporter.id = :reporterId")
    void deleteByReporterId(@Param("reporterId") Long reporterId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM ArtistGalleryPhotoReport apr WHERE apr.photo.artist.id = :artistId")
    void deleteAllByPhotoArtistId(@Param("artistId") Long artistId);

    @Query("SELECT apr.photo.uploader.id, COUNT(apr) FROM ArtistGalleryPhotoReport apr WHERE apr.photo.uploader.id IN :userIds GROUP BY apr.photo.uploader.id")
    List<Object[]> countByPhotoUploaderIds(@Param("userIds") Collection<Long> userIds);

    @EntityGraph(attributePaths = {"photo", "photo.artist", "photo.uploader", "reporter"})
    @Query("SELECT apr FROM ArtistGalleryPhotoReport apr ORDER BY apr.createdAt DESC")
    List<ArtistGalleryPhotoReport> findAllForExport(Pageable pageable);
}
