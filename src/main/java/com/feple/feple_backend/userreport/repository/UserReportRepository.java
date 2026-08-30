package com.feple.feple_backend.userreport.repository;

import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.repository.BaseReportRepository;
import com.feple.feple_backend.userreport.entity.UserReport;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserReportRepository extends BaseReportRepository<UserReport> {

    @Query("SELECT CASE WHEN COUNT(ur) > 0 THEN TRUE ELSE FALSE END FROM UserReport ur WHERE ur.reporter.id = :reporterId AND ur.target.id = :targetId")
    boolean existsByReporterIdAndTargetId(@Param("reporterId") Long reporterId, @Param("targetId") Long targetId);

    @Override
    @EntityGraph(attributePaths = {"target", "reporter"})
    Page<UserReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"target", "reporter"})
    Page<UserReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"target", "reporter"})
    @Query("SELECT ur FROM UserReport ur WHERE " +
           "(:status IS NULL OR ur.status = :status) AND " +
           "(LOWER(ur.target.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' OR " +
           " LOWER(ur.reporter.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!') " +
           "ORDER BY ur.createdAt DESC")
    Page<UserReport> searchByKeyword(@Param("keyword") String keyword,
                                      @Param("status") ReportStatus status,
                                      Pageable pageable);

    @Query("SELECT ur.target.id, COUNT(ur) FROM UserReport ur WHERE ur.target.id IN :userIds GROUP BY ur.target.id")
    List<Object[]> countByTargetIds(@Param("userIds") Collection<Long> userIds);

    // target이 @ManyToOne이라 파생 쿼리(findByTargetId)는 Hibernate 6에서 시작 시
    // PathElementException 발생 — @Query로 명시해야 함(CLAUDE.md 문서화된 패턴).
    @Query("SELECT ur FROM UserReport ur WHERE ur.target.id = :targetId")
    List<UserReport> findByTargetId(@Param("targetId") Long targetId);

    // 회원 완전 삭제 전용 — 이 유저가 신고자이거나 피신고자인 신고 행을 모두 제거한다.
    @Modifying
    @Query("DELETE FROM UserReport ur WHERE ur.reporter.id = :userId OR ur.target.id = :userId")
    void deleteByUserInvolved(@Param("userId") Long userId);
}
