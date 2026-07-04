package com.feple.feple_backend.admin.log;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.global.LikeEscaper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AdminAction action, String targetType, Long targetId, String detail) {
        String adminUsername = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                adminUsername = auth.getName();
            }
        } catch (Exception ignored) {}
        try {
            repository.save(AdminLog.builder()
                    .adminUsername(adminUsername)
                    .action(action.name())
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(detail)
                    .ipAddress(extractClientIp())
                    .build());
        } catch (Exception e) {
            log.error("감사 로그 저장 실패: action={}, targetType={}, targetId={}", action.name(), targetType, targetId, e);
        }
    }

    private String extractClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            // 리버스 프록시(nginx/ALB) 환경: X-Forwarded-For 첫 번째 값이 실제 클라이언트 IP
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].strip();
            }
            return req.getRemoteAddr();
        } catch (Exception ignored) {
            return null;
        }
    }

    public Page<AdminLog> getLogs(int page, AdminLogFilter filter) {
        PageRequest pageable = PageRequest.of(page, AdminConstants.LOG_PAGE_SIZE);
        String type     = !filter.targetType().isBlank() ? filter.targetType() : null;
        String username = LikeEscaper.escapeOrNull(filter.adminUsername());
        LocalDateTime fromDt = filter.from() != null ? filter.from().atStartOfDay() : null;
        LocalDateTime toDt   = filter.to()   != null ? filter.to().atTime(LocalTime.MAX) : null;
        return repository.findWithFilters(type, username, fromDt, toDt, pageable);
    }

    public List<AdminLog> getRecentLogs() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }
}
