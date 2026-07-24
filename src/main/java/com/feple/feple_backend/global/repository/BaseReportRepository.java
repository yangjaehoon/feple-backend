package com.feple.feple_backend.global.repository;

import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 신고 유형 Repository 공통 계약.
 * countByStatus는 파생 쿼리로 자동 생성되므로 구현체에서 재선언 불필요.
 * 나머지 3개 메서드는 entity별 @EntityGraph/@Query 가 필요하므로 구현체에서 @Override 재선언.
 */
@NoRepositoryBean
public interface BaseReportRepository<T> extends JpaRepository<T, Long> {
    long countByStatus(ReportStatus status);
    Page<T> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);
    Page<T> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<T> searchByKeyword(String keyword, ReportStatus status, Pageable pageable);
}
