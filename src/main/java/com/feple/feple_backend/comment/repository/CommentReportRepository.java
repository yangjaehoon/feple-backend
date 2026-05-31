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

import java.util.Collection;
import java.util.List;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    @Query("SELECT CASE WHEN COUNT(cr) > 0 THEN TRUE ELSE FALSE END FROM CommentReport cr WHERE cr.reporter.id = :reporterId AND cr.comment.id = :commentId")
    boolean existsByReporterIdAndCommentId(@Param("reporterId") Long reporterId, @Param("commentId") Long commentId);

    @EntityGraph(attributePaths = {"comment", "comment.user", "comment.post", "reporter"})
    Page<CommentReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"comment", "comment.user", "comment.post", "reporter"})
    Page<CommentReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"comment", "comment.user", "comment.post", "reporter"})
    @Query("SELECT cr FROM CommentReport cr WHERE " +
           "(:status IS NULL OR cr.status = :status) AND " +
           "(LOWER(cr.comment.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(cr.reporter.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY cr.createdAt DESC")
    Page<CommentReport> searchByKeyword(@Param("keyword") String keyword,
                                        @Param("status") ReportStatus status,
                                        Pageable pageable);

    @EntityGraph(attributePaths = {"comment", "comment.user", "comment.post", "reporter"})
    @Query("SELECT cr FROM CommentReport cr ORDER BY cr.createdAt DESC")
    List<CommentReport> findAllForExport();

    long countByStatus(ReportStatus status);

    @Modifying
    @Query("DELETE FROM CommentReport cr WHERE cr.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);

    @Query("SELECT cr.comment.user.id, COUNT(cr) FROM CommentReport cr WHERE cr.comment.user.id IN :userIds GROUP BY cr.comment.user.id")
    List<Object[]> countByCommentAuthorIds(@Param("userIds") Collection<Long> userIds);
}
