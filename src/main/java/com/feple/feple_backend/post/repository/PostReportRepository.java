package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.global.repository.BaseReportRepository;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface PostReportRepository extends BaseReportRepository<PostReport> {

    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN TRUE ELSE FALSE END FROM PostReport pr WHERE pr.reporter.id = :reporterId AND pr.post.id = :postId")
    boolean existsByReporterIdAndPostId(@Param("reporterId") Long reporterId, @Param("postId") Long postId);

    @Override
    @EntityGraph(attributePaths = {"post", "post.user", "reporter"})
    Page<PostReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"post", "post.user", "reporter"})
    Page<PostReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"post", "post.user", "reporter"})
    @Query("SELECT pr FROM PostReport pr WHERE " +
           "(:status IS NULL OR pr.status = :status) AND " +
           "(LOWER(pr.post.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           " LOWER(pr.reporter.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
           "ORDER BY pr.createdAt DESC")
    Page<PostReport> searchByKeyword(@Param("keyword") String keyword,
                                     @Param("status") ReportStatus status,
                                     Pageable pageable);

    @EntityGraph(attributePaths = {"post", "post.user", "reporter"})
    @Query("SELECT pr FROM PostReport pr ORDER BY pr.createdAt DESC")
    List<PostReport> findAllForExport(org.springframework.data.domain.Pageable pageable);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Query("SELECT FUNCTION('DATE', pr.createdAt), COUNT(pr) FROM PostReport pr " +
           "WHERE pr.createdAt >= :from AND pr.createdAt < :to GROUP BY FUNCTION('DATE', pr.createdAt)")
    java.util.List<Object[]> countGroupByDate(@Param("from") java.time.LocalDateTime from,
                                              @Param("to") java.time.LocalDateTime to);

    @Query("SELECT pr FROM PostReport pr WHERE pr.post.id = :postId")
    List<PostReport> findByPostId(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostReport pr WHERE pr.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostReport pr WHERE pr.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT pr.post.user.id, COUNT(pr) FROM PostReport pr WHERE pr.post.user.id IN :userIds GROUP BY pr.post.user.id")
    List<Object[]> countByPostAuthorIds(@Param("userIds") Collection<Long> userIds);
}
