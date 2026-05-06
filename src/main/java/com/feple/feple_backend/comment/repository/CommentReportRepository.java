package com.feple.feple_backend.comment.repository;

import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByReporterIdAndCommentId(Long reporterId, Long commentId);

    @EntityGraph(attributePaths = {"comment", "comment.user", "comment.post", "reporter"})
    Page<CommentReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"comment", "comment.user", "comment.post", "reporter"})
    Page<CommentReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);

    void deleteByCommentId(Long commentId);
}
