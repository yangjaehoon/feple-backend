package com.feple.feple_backend.user.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

// 로그인 이벤트가 아니라 인증된 요청 기준으로 "오늘 이미 접속을 기록했는지"만 판단한다 — 세션이 유지된
// 채 재로그인 없이 접속하는 사용자도 하루 1행으로 정확히 집계하기 위함. 캐시 체크·적재를 이 메서드
// 자신의(요청) 스레드에서 동기로 먼저 끝내고, 실제 DB 기록만 UserAccessLogWriter로 위임해 비동기
// 처리한다 — 같은 객체 안에서 @Async 메서드를 self-invocation하면 프록시를 우회해 비동기가 무시되므로
// 반드시 별도 빈으로 분리해야 한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccessTrackingService {

    private final UserAccessLogWriter userAccessLogWriter;

    private final Cache<String, Boolean> recordedToday = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(26))
            .maximumSize(200_000)
            .build();

    public void recordAccess(Long userId) {
        LocalDate today = LocalDate.now();
        String cacheKey = userId + ":" + today;
        if (recordedToday.getIfPresent(cacheKey) != null) {
            return;
        }
        // 비동기 작업 완료를 기다리지 않고 먼저 캐시에 적재 — 동시 요청이 같은 유저에 대해
        // 중복으로 비동기 작업을 제출하는 것을 막는다(DB에는 어차피 INSERT IGNORE라 안전하지만,
        // accessLogExecutor 풀에 불필요한 제출이 몰리는 것 자체를 줄이기 위함).
        recordedToday.put(cacheKey, Boolean.TRUE);

        try {
            userAccessLogWriter.persist(userId, today);
        } catch (TaskRejectedException e) {
            // accessLogExecutor의 스레드+큐가 모두 찼을 때 Spring이 호출 스레드(=이 필터 요청 스레드)로
            // 동기적으로 던진다 — 접속 기록은 부가 지표일 뿐이므로 여기서 삼켜 요청 처리를 막지 않는다.
            log.warn("[UserAccessTracking] 접속 기록 작업이 풀 포화로 거부됨 userId={}", userId, e);
        }
    }
}
