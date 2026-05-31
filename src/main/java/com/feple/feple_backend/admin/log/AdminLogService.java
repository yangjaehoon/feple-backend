package com.feple.feple_backend.admin.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
                    .build());
        } catch (Exception e) {
            log.error("감사 로그 저장 실패: action={}, targetType={}, targetId={}", action, targetType, targetId, e);
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
