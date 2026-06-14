package com.feple.feple_backend.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
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
            } catch (Exception ignored) {
                log.info("[Firebase] 공개키 캐시 워밍업 완료");
            }
        });
    }
}
