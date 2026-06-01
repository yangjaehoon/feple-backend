package com.feple.feple_backend.admin.log;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository repository;

    public void log(String action, String targetType, Long targetId, String detail) {
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
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(detail)
                    .ipAddress(extractClientIp())
                    .build());
        } catch (Exception e) {
            log.error("감사 로그 저장 실패: action={}, targetType={}, targetId={}", action, targetType, targetId, e);
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

    public Page<AdminLog> getLogs(int page, int size, String targetType, String adminUsername, LocalDate from, LocalDate to) {
        PageRequest pageable = PageRequest.of(page, size);
        String type     = (targetType     != null && !targetType.isBlank())     ? targetType     : null;
        String username = (adminUsername  != null && !adminUsername.isBlank())  ? adminUsername  : null;
        LocalDateTime fromDt = from != null ? from.atStartOfDay()      : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59)    : null;
        return repository.findWithFilters(type, username, fromDt, toDt, pageable);
    }

    public List<AdminLog> getRecentLogs() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }
}
