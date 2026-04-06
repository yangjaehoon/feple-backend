package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.entity.PasswordResetToken;
import com.feple.feple_backend.auth.repository.PasswordResetTokenRepository;
import com.feple.feple_backend.user.domain.AuthProvider;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    private final SecureRandom secureRandom = new SecureRandom();

    /** 비밀번호 재설정 이메일 요청 */
    @Transactional
    public void requestReset(String email) {
        // 사용자 존재 여부와 무관하게 동일한 응답 반환 (이메일 열거 공격 방지)
        userRepository.findByProviderAndOauthId(AuthProvider.EMAIL, email).ifPresent(user -> {
            // 기존 토큰 삭제
            tokenRepository.deleteByUserId(user.getId());

            // 안전한 랜덤 토큰 생성 (32바이트 → URL-safe Base64)
            byte[] raw = new byte[32];
            secureRandom.nextBytes(raw);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

            // 해시 저장
            String tokenHash = hash(rawToken);
            tokenRepository.save(PasswordResetToken.of(user.getId(), tokenHash));

            // 이메일 발송
            String resetLink = baseUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(email, resetLink);
        });
    }

    /** 토큰 유효성 확인 (웹 폼 표시 전 체크) */
    @Transactional(readOnly = true)
    public boolean isValidToken(String rawToken) {
        return tokenRepository.findByTokenHash(hash(rawToken))
                .map(t -> !t.isExpired())
                .orElse(false);
    }

    /** 비밀번호 재설정 */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = hash(rawToken);
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new IllegalArgumentException("만료된 토큰입니다. 비밀번호 재설정을 다시 요청해주세요.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.changePassword(passwordEncoder.encode(newPassword));
        tokenRepository.delete(token); // 사용한 토큰 즉시 삭제
        log.info("[PasswordReset] userId={} 비밀번호 재설정 완료", user.getId());
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** 매일 새벽 3시 30분에 만료 토큰 정리 */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
