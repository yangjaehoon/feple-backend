package com.feple.feple_backend.userreport.service;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.ReportRejectionService;
import com.feple.feple_backend.global.cache.EvictAdminReportCaches;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.user.service.UserAdminService;
import com.feple.feple_backend.userreport.entity.UserReport;
import com.feple.feple_backend.userreport.repository.UserReportRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReportService implements ReportAdminService<UserReport> {

    private final UserReportRepository reportRepository;
    private final UserRepository userRepository;
    private final UserAdminService userAdminService;

    // 다른 신고 유형(PostReportService 등)의 submitReport와 구조가 동일하지만,
    // 빌더 타입이 전부 달라 제네릭으로 묶으면 콜백만 많아지고 오히려 읽기 어려워져 통합하지 않는다.
    @Transactional
    @EvictAdminReportCaches
    public void submitReport(Long targetId, Long reporterId, ReportSubmitRequest command) {
        if (targetId.equals(reporterId)) {
            throw new IllegalArgumentException("자기 자신을 신고할 수 없습니다.");
        }
        if (reportRepository.existsByReporterIdAndTargetId(reporterId, targetId)) {
            throw new ConflictException("이미 신고한 사용자입니다.");
        }
        User target = EntityLoader.getOrThrow(userRepository::findById, targetId, "사용자");
        User reporter = EntityLoader.getOrThrow(userRepository::findById, reporterId, "사용자");

        // existsBy 체크 후 save() 사이의 TOCTOU 레이스(동시 중복 신고)는 유니크 제약
        // (reporter_id, target_id)이 최종 방어선이다 — 위 existsBy와 동일한 메시지의
        // ConflictException으로 변환해준다.
        try {
            reportRepository.save(UserReport.builder()
                    .target(target)
                    .reporter(reporter)
                    .reason(command.reason())
                    .detail(command.detail())
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("이미 신고한 사용자입니다.");
        }
    }

    @Override
    @Cacheable(value = "adminReportTypeCounts", key = "'userPending'")
    public long getPendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Override
    @Cacheable(value = "adminReportTypeCounts", key = "'userTotal'")
    public long getTotalCount() {
        return reportRepository.count();
    }

    @Override
    public String getReportType() { return AdminConstants.REPORT_TYPE_USER; }

    @Override
    public Page<UserReport> findPendingReports(PageRequest pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
    }

    @Override
    public Page<UserReport> findAllReports(PageRequest pageable) {
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<UserReport> searchReportsByKeyword(String keyword, ReportStatus status, PageRequest pageable) {
        return reportRepository.searchByKeyword(keyword, status, pageable);
    }

    // "삭제" 액션 = 신고된 유저 계정을 관리자 권한으로 탈퇴 처리(기존 UserAdminService
    // 재사용 — 별도 정지 기간을 새로 발명하지 않음). 같은 유저를 대상으로 한 다른
    // 대기 신고도 함께 처리 완료로 갱신한다.
    @Override
    @EvictAdminReportCaches
    @Transactional
    public void deleteContentAndResolve(Long reportId) {
        UserReport report = EntityLoader.getOrThrow(reportRepository::findById, reportId, "신고");
        Long targetId = report.getTargetId();
        userAdminService.adminDeleteUser(targetId);
        reportRepository.findByTargetId(targetId).stream()
                .filter(UserReport::isPending)
                .forEach(r -> r.resolve(ReportStatus.USER_DELETED));
    }

    @EvictAdminReportCaches
    @Transactional
    public void dismissReport(Long reportId) {
        ReportRejectionService.reject(reportRepository, reportId);
    }

    @Override
    @EvictAdminReportCaches
    @Transactional
    public void bulkDismiss(List<Long> ids) {
        ReportRejectionService.bulkDismiss(reportRepository, ids);
    }

    @Override
    public Long extractAuthorId(UserReport report) { return report.getTargetId(); }

    @Override
    public Map<Long, Long> getAuthorReportCounts(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return QueryResultMapper.toLongMap(reportRepository.countByTargetIds(userIds));
    }

    public long getReportCountForUser(Long userId) {
        return QueryResultMapper.extractSingleCount(reportRepository.countByTargetIds(List.of(userId)));
    }
}
