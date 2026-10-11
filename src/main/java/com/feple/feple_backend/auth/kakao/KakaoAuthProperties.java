package com.feple.feple_backend.auth.kakao;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 카카오 로그인 검증 설정. {@code appId}는 카카오 디벨로퍼스 [앱 설정 > 일반]의 "앱 ID"(숫자)다 —
 * 네이티브/JS 앱 키와 다르다. 미설정이면 기동이 실패한다 — 요청 시점에야 드러나면 배포가
 * 헬스체크를 통과한 채 카카오 로그인만 전부 500이 된다.
 */
@Validated
@ConfigurationProperties(prefix = "app.kakao")
public record KakaoAuthProperties(@NotNull Long appId) {
}
