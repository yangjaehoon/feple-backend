package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.entity.RefreshToken;
import com.feple.feple_backend.auth.jwt.JwtProperties;
import com.feple.feple_backend.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    /** 토큰 → SHA-256 해시 (16진수 문자열) */
    public String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Transactional
    public void save(Long userId, String rawToken) {
        String tokenHash = hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.refreshTokenExpirationMs() / 1000);
        refreshTokenRepository.deleteByUserId(userId); // 기존 토큰 교체
        refreshTokenRepository.save(RefreshToken.of(userId, tokenHash, expiresAt));
    }

    /**
     * 기존 리프레시 토큰을 검증하고 삭제한 뒤 userId를 반환한다.
     * 새 토큰 저장은 호출자가 save()를 통해 처리한다.
     */
    @Transactional
    public Long validateAndConsume(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다."));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("만료된 리프레시 토큰입니다. 다시 로그인해주세요.");
        }

        Long userId = stored.getUserId();
        refreshTokenRepository.delete(stored); // 사용한 토큰 즉시 삭제 (로테이션)
        return userId;
    }

    @Transactional
    public void revoke(String rawToken) {
        String tokenHash = hash(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /** 매일 새벽 3시에 만료된 토큰 정리 */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
