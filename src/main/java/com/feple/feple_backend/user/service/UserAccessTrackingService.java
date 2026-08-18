package com.feple.feple_backend.user.service;

import com.feple.feple_backend.user.repository.UserAccessLogRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// 로그인 이벤트가 아니라 인증된 요청 기준으로 "오늘 이미 접속을 기록했는지"만 판단한다 — 세션이 유지된
// 채 재로그인 없이 접속하는 사용자도 하루 1행으로 정확히 집계하기 위함. userId+날짜 키가 캐시에 있으면
// DB를 아예 건드리지 않아, 사실상 유저별 하루 1회만 INSERT 시도가 발생한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccessTrackingService {

    private final UserAccessLogRepository userAccessLogRepository;

    private final Cache<String, Boolean> recordedToday = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(26))
            .maximumSize(200_000)
            .build();

    @Async("accessLogExecutor")
    public void recordAccess(Long userId) {
        LocalDate today = LocalDate.now();
        String cacheKey = userId + ":" + today;
        if (recordedToday.getIfPresent(cacheKey) != null) {
            return;
        }

        try {
            userAccessLogRepository.insertIgnore(userId, today, LocalDateTime.now());
            recordedToday.put(cacheKey, Boolean.TRUE);
        } catch (Exception e) {
            log.warn("[UserAccessTracking] 접속 기록 실패 userId={}", userId, e);
        }
    }
}
