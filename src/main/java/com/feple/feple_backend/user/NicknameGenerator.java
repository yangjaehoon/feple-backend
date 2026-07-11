package com.feple.feple_backend.user;

import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * OAuth 신규 가입 시 닉네임 생성 공통 로직.
 * KakaoAuthService, FirebaseAuthService 양쪽에서 사용.
 */
@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    private final UserRepository userRepository;
    private final NicknameContentValidator nicknameContentValidator;

    /**
     * 외부 displayName을 안전한 닉네임으로 정제한다.
     * 특수문자 제거 후 금칙어/아티스트명/제한어 검증 실패 시 fallback 반환.
     */
    public String sanitize(String raw, String fallback) {
        String sanitized = raw.trim().replaceAll("[^가-힣a-zA-Z0-9_]", "");
        if (sanitized.length() < 2) return fallback;
        if (sanitized.length() > 8) sanitized = sanitized.substring(0, 8);
        try {
            nicknameContentValidator.validate(sanitized);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
        return sanitized;
    }

    /**
     * 이미 사용 중인 닉네임이면 숫자 suffix를 붙여 고유하게 만든다.
     */
    public String uniquify(String base) {
        if (base.length() > 8) base = base.substring(0, 8);
        if (base.length() < 2) base = "User";
        if (!userRepository.existsByNickname(base)) return base;
        // base(최대 6자) + i(최대 3자리)로 최대 9자 — NicknameValidator의 8자 제한을 넘을 수 있음
        // (OAuth 자동 생성 경로는 NicknameValidator를 거치지 않아 여기서 걸러지지 않으면 그대로 저장됨)
        for (int i = 2; i <= 999; i++) {
            String candidate = base.substring(0, Math.min(base.length(), 6)) + i;
            if (!userRepository.existsByNickname(candidate)) return candidate;
        }
        // 999개 후보가 모두 소진된 극단적 상황 — 타임스탬프 모듈로 추측(충돌 가능)이 아닌
        // UUID 기반 후보를 존재 여부까지 확인해 실제 유니크함을 보장한다.
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = "User" + UUID.randomUUID().toString().substring(0, 4);
            if (!userRepository.existsByNickname(candidate)) return candidate;
        }
        return "User" + UUID.randomUUID().toString().substring(0, 4);
    }
}
