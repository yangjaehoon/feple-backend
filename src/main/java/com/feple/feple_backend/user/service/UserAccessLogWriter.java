package com.feple.feple_backend.user.service;

import com.feple.feple_backend.user.repository.UserAccessLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// UserAccessTrackingService에서 self-invocation으로 @Async를 걸 수 없어 분리한 전용 쓰기 빈.
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccessLogWriter {

    private final UserAccessLogRepository userAccessLogRepository;

    @Async("accessLogExecutor")
    public void persist(Long userId, LocalDate accessDate) {
        try {
            userAccessLogRepository.insertIgnore(userId, accessDate, LocalDateTime.now());
        } catch (Exception e) {
            log.warn("[UserAccessTracking] 접속 기록 실패 userId={}", userId, e);
        }
    }
}
