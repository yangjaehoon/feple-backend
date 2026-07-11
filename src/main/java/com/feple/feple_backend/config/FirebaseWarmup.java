package com.feple.feple_backend.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class FirebaseWarmup {

    // 형식은 유효한 JWT(alg=RS256, kid=warmup, payload={})이나 서명이 잘못된 토큰.
    // verifyIdToken 내부에서 서명 검증 전에 Google 공개키를 fetch·캐시하므로
    // 항상 예외가 발생하지만 공개키는 캐시됨 — 첫 로그인 ~15초 지연 해소가 목적.
    private static final String WARMUP_TOKEN =
            "eyJhbGciOiJSUzI1NiIsImtpZCI6Indhcm11cCJ9.e30.placeholder";

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpOnStart() {
        triggerWarmup();
    }

    // Google 공개키 Cache-Control max-age = 6시간. 5시간마다 갱신해 캐시 만료 후
    // 첫 로그인이 다시 느려지는 현상을 방지한다.
    @Scheduled(fixedDelayString = "PT5H", initialDelayString = "PT5H")
    public void warmUpPeriodically() {
        triggerWarmup();
    }

    private void triggerWarmup() {
        if (FirebaseApp.getApps().isEmpty()) return;
        CompletableFuture.runAsync(() -> {
            try {
                FirebaseAuth.getInstance().verifyIdToken(WARMUP_TOKEN);
                log.warn("[Firebase] 워밍업 토큰이 검증을 통과했습니다 — WARMUP_TOKEN을 다시 생성하세요.");
            } catch (FirebaseAuthException e) {
                // CERTIFICATE_FETCH_FAILED: 공개키 fetch 자체가 실패 — 워밍업 효과 없음.
                // 그 외 코드(INVALID_ID_TOKEN 등): fetch는 성공, 서명 검증 단계에서 예상대로 거부됨.
                if (e.getAuthErrorCode() == AuthErrorCode.CERTIFICATE_FETCH_FAILED) {
                    log.warn("[Firebase] 공개키 캐시 워밍업 실패 — 공개키 fetch 자체가 안 됨", e);
                } else {
                    log.info("[Firebase] 공개키 캐시 워밍업 완료 (errorCode={})", e.getAuthErrorCode());
                }
            } catch (Exception e) {
                log.warn("[Firebase] 공개키 캐시 워밍업 중 예상치 못한 예외", e);
            }
        });
    }
}
