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

    public Page<AdminLog> getLogs(int page, int size, String targetType, LocalDate from, LocalDate to) {
        PageRequest pageable = PageRequest.of(page, size);
        boolean hasType = targetType != null && !targetType.isBlank();
        boolean hasDate = from != null || to != null;

        if (!hasDate) {
            return hasType
                    ? repository.findByTargetTypeOrderByCreatedAtDesc(targetType, pageable)
                    : repository.findAllByOrderByCreatedAtDesc(pageable);
        }

        LocalDateTime fromDt = (from != null ? from : LocalDate.of(2000, 1, 1)).atStartOfDay();
        LocalDateTime toDt   = (to   != null ? to   : LocalDate.now()).atTime(23, 59, 59);
        return hasType
                ? repository.findByTargetTypeAndDateRange(targetType, fromDt, toDt, pageable)
                : repository.findByDateRange(fromDt, toDt, pageable);
    }

    public List<AdminLog> getRecentLogs() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }
}
