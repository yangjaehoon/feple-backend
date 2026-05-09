package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN TRUE ELSE FALSE END FROM PostReport pr WHERE pr.reporter.id = :reporterId AND pr.post.id = :postId")
    boolean existsByReporterIdAndPostId(@Param("reporterId") Long reporterId, @Param("postId") Long postId);

    @EntityGraph(attributePaths = {"post", "post.user", "reporter"})
    Page<PostReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"post", "post.user", "reporter"})
    Page<PostReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Modifying
    @Query("DELETE FROM PostReport pr WHERE pr.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
