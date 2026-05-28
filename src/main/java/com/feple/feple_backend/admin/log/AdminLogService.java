package com.feple.feple_backend.admin.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository repository;

    public void log(String action, String targetType, Long targetId, String detail) {
        try {
            repository.save(AdminLog.builder()
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .detail(detail)
                    .build());
        } catch (Exception e) {
            log.error("감사 로그 저장 실패: action={}, targetType={}, targetId={}", action, targetType, targetId, e);
        }
    }

    public Page<AdminLog> getLogs(int page, int size, String targetType) {
        PageRequest pageable = PageRequest.of(page, size);
        if (targetType != null && !targetType.isBlank()) {
            return repository.findByTargetTypeOrderByCreatedAtDesc(targetType, pageable);
        }
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<AdminLog> getRecentLogs() {
        return repository.findTop10ByOrderByCreatedAtDesc();
    }
}
