package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    boolean existsByReporterIdAndPostId(Long reporterId, Long postId);

    @EntityGraph(attributePaths = {"post", "post.user", "reporter"})
    Page<PostReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"post", "post.user", "reporter"})
    Page<PostReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    void deleteByPostId(Long postId);
}
