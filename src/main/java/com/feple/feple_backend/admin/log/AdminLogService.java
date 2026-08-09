package com.feple.feple_backend.admin.log;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.CurrentAdminProvider;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository repository;
    private final CurrentAdminProvider currentAdminProvider;

    // REQUIRES_NEW: 호출 측 트랜잭션이 롤백되더라도 감사 로그는 별도 트랜잭션으로 반드시 커밋한다.
    // 예) 아티스트 삭제 중 예외 → 삭제 트랜잭션은 롤백되지만 "삭제 시도" 로그는 DB에 남아야 함.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AdminAction action, String targetType, Long targetId, String detail) {
        String adminUsername = currentAdminProvider.usernameOrNull();
        try {
            repository.save(AdminLog.builder()
                    .adminUsername(adminUsername)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(detail)
                    .ipAddress(extractClientIp())
                    .build());
        } catch (Exception e) {
            // 감사 로그 저장 실패가 관리자 액션 자체를 중단시켜선 안 됨 — fail-safe
            log.error("감사 로그 저장 실패: action={}, targetType={}, targetId={}", action, targetType, targetId, e);
        }
    }

    private String extractClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            // 리버스 프록시 없이 JVM이 직접 트래픽을 받으므로 remoteAddr이 곧 실제 클라이언트 IP —
            // X-Forwarded-For는 신뢰하지 않는다(관리자가 조작하면 감사 로그의 IP를 위조할 수 있음)
            return attrs.getRequest().getRemoteAddr();
        } catch (Exception ignored) {
            return null;
        }
    }

    public Page<AdminLog> getLogs(int page, AdminLogFilter filter) {
        PageRequest pageable = PageRequest.of(page, AdminConstants.LOG_PAGE_SIZE);
        String type     = !filter.targetType().isBlank() ? filter.targetType() : null;
        String username = JpqlLikeEscaper.escapeOrNull(filter.adminUsername());
        LocalDateTime fromDt = filter.from() != null ? filter.from().atStartOfDay() : null;
        LocalDateTime toDt   = filter.to()   != null ? filter.to().atTime(LocalTime.MAX) : null;
        return repository.findWithFilters(type, username, fromDt, toDt, pageable);
    }

    public List<AdminLog> getRecentLogs() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }
}
