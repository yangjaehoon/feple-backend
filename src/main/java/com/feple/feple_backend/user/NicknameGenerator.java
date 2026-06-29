package com.feple.feple_backend.user;

import com.feple.feple_backend.artist.ArtistNameFilter;
import com.feple.feple_backend.badword.BadWordFilter;
import com.feple.feple_backend.nickname.NicknameRestrictionFilter;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OAuth 신규 가입 시 닉네임 생성 공통 로직.
 * KakaoAuthService, FirebaseAuthService 양쪽에서 사용.
 */
@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    private final UserRepository userRepository;
    private final BadWordFilter badWordFilter;
    private final ArtistNameFilter artistNameFilter;
    private final NicknameRestrictionFilter nicknameRestrictionFilter;

    /**
     * 외부 displayName을 안전한 닉네임으로 정제한다.
     * 특수문자 제거 후 금칙어/아티스트명/제한어 검증 실패 시 fallback 반환.
     */
    public String sanitize(String raw, String fallback) {
        String sanitized = raw.trim().replaceAll("[^가-힣a-zA-Z0-9_]", "");
        if (sanitized.length() < 2) return fallback;
        if (sanitized.length() > 8) sanitized = sanitized.substring(0, 8);
        try {
            badWordFilter.validate(sanitized);
            artistNameFilter.validate(sanitized);
            nicknameRestrictionFilter.validate(sanitized);
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
        for (int i = 2; i <= 999; i++) {
            String candidate = base.substring(0, Math.min(base.length(), 6)) + i;
            if (!userRepository.existsByNickname(candidate)) return candidate;
        }
        return base + System.currentTimeMillis() % 10000;
    }
}
