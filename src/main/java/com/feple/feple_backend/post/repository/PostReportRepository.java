package com.feple.feple_backend.post.repository;

import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.repository.BaseReportRepository;
import com.feple.feple_backend.post.entity.PostReport;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostReportRepository extends BaseReportRepository<PostReport> {

    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN TRUE ELSE FALSE END FROM PostReport pr WHERE pr.reporter.id = :reporterId AND pr.post.id = :postId")
    boolean existsByReporterIdAndPostId(@Param("reporterId") Long reporterId, @Param("postId") Long postId);

    // 회원 완전 삭제(hardDelete) 시 users 행 물리 삭제 전에 이 유저가 낸 신고를 비운다 (reporter_id FK RESTRICT).
    @Modifying
    @Transactional
    @Query("DELETE FROM PostReport pr WHERE pr.reporter.id = :reporterId")
    void deleteByReporterId(@Param("reporterId") Long reporterId);

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
    List<PostReport> findAllForExport(Pageable pageable);

    @Query("SELECT FUNCTION('DATE', pr.createdAt), COUNT(pr) FROM PostReport pr " +
           "WHERE pr.createdAt >= :from AND pr.createdAt < :to GROUP BY FUNCTION('DATE', pr.createdAt)")
    List<Object[]> countGroupByDate(@Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    @Query("SELECT pr FROM PostReport pr WHERE pr.post.id = :postId")
    List<PostReport> findByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(pr) FROM PostReport pr WHERE pr.post.id = :postId AND pr.status = :status")
    long countByPostIdAndStatus(@Param("postId") Long postId, @Param("status") ReportStatus status);

    // bulkDismiss처럼 여러 postId의 대기 신고 수를 한 번에 확인해야 할 때, 항목마다 countByPostIdAndStatus를
    // 반복 호출하지 않도록 그룹 집계로 한 번에 조회한다.
    @Query("SELECT pr.post.id, COUNT(pr) FROM PostReport pr WHERE pr.post.id IN :postIds AND pr.status = :status GROUP BY pr.post.id")
    List<Object[]> countByPostIdInAndStatus(@Param("postIds") Collection<Long> postIds, @Param("status") ReportStatus status);

    @Query("SELECT pr.post.user.id, COUNT(pr) FROM PostReport pr WHERE pr.post.user.id IN :userIds GROUP BY pr.post.user.id")
    List<Object[]> countByPostAuthorIds(@Param("userIds") Collection<Long> userIds);
}
