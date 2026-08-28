package com.feple.feple_backend.userreport.service;

import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.ReportRejectionService;
import com.feple.feple_backend.global.ReportTypes;
import com.feple.feple_backend.global.cache.EvictAdminReportCaches;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.global.exception.InvalidRequestException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
            throw new InvalidRequestException("자기 자신을 신고할 수 없습니다.");
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
    public String getReportType() { return ReportTypes.USER; }

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
    //
    // 이미 처리된(비대기) 신고면 여기서 멈춘다 — 같은 유저를 대상으로 한 신고 여러
    // 건을 한꺼번에 벌크 삭제하면, 첫 건 처리 때 이미 나머지 신고들도 USER_DELETED로
    // 정리되므로, 가드가 없으면 이후 항목들이 adminDeleteUser를 다시 호출해 이미
    // 탈퇴 처리된 유저를 또 처리하게 된다(User는 Post/Comment와 달리 소프트 삭제된
    // 행도 findById로 그대로 조회되어 예외 없이 조용히 재실행됨 — deletedAt이
    // 재처리 시각으로 덮어써짐).
    @Override
    @EvictAdminReportCaches
    @Transactional
    public void deleteContentAndResolve(Long reportId) {
        UserReport report = EntityLoader.getOrThrow(reportRepository::findById, reportId, "신고");
        if (!report.isPending()) {
            log.info("[UserReport] 이미 처리된 신고라 건너뜀 reportId={}", reportId);
            return;
        }
        Long targetId = report.getTargetId();
        userAdminService.adminDeleteUser(targetId);
        List<UserReport> resolved = reportRepository.findByTargetId(targetId).stream()
                .filter(UserReport::isPending)
                .toList();
        resolved.forEach(r -> r.resolve(ReportStatus.USER_DELETED));
        if (resolved.size() > 1) {
            log.info("[UserReport] 유저 탈퇴 처리로 신고 {}건 함께 정리됨 targetId={} triggeredBy={}",
                    resolved.size(), targetId, reportId);
        }
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
