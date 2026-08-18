package com.feple.feple_backend.userreport.repository;

import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.repository.BaseReportRepository;
import com.feple.feple_backend.userreport.entity.UserReport;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    List<UserReport> findByTargetId(Long targetId);
}
