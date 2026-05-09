package com.feple.feple_backend.comment.repository;

import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN TRUE ELSE FALSE END FROM CommentReport cr WHERE cr.reporter.id = :reporterId AND cr.comment.id = :commentId")
    boolean existsByReporterIdAndCommentId(@Param("reporterId") Long reporterId, @Param("commentId") Long commentId);

    @EntityGraph(attributePaths = {"comment", "comment.user", "comment.post", "reporter"})
    Page<CommentReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"comment", "comment.user", "comment.post", "reporter"})
    Page<CommentReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);

    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);
}
