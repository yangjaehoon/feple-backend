package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.entity.RefreshToken;
import com.feple.feple_backend.auth.jwt.JwtProperties;
import com.feple.feple_backend.auth.repository.RefreshTokenRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("토큰 저장 중 사용자를 찾을 수 없습니다."));
        String tokenHash = hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.refreshTokenExpirationMs() / 1000);
        refreshTokenRepository.deleteByUserId(userId); // 기존 토큰 교체
        refreshTokenRepository.save(RefreshToken.of(user, tokenHash, expiresAt));
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
            refreshTokenRepository.deleteByTokenHash(tokenHash);
            throw new IllegalArgumentException("만료된 리프레시 토큰입니다. 다시 로그인해주세요.");
        }

        Long userId = stored.getUserId();
        // 직접 DELETE 쿼리로 삭제: 동시 요청이 같은 토큰을 소비하려 할 때 0이 반환됨
        int deleted = refreshTokenRepository.deleteByTokenHash(tokenHash);
        if (deleted == 0) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }
        return userId;
    }

    public record RotationResult(Long userId, String newRefreshToken) {}

    /**
     * 기존 리프레시 토큰을 검증·소비하고 새 토큰을 발급·저장하는 과정을 하나의 트랜잭션으로 묶는다.
     * validateAndConsume()과 save()를 컨트롤러에서 별도 호출하면, 그 사이(혹은 save() 자체)에서
     * 실패할 경우 기존 토큰은 이미 삭제됐는데 새 토큰은 발급되지 않아 사용자가 재로그인 외에는
     * 복구할 수 없는 상태가 될 수 있다 — 실패 시 전체 롤백되어 기존 토큰이 보존되도록 한다.
     */
    @Transactional
    public RotationResult rotate(String oldRawToken, Function<Long, String> newTokenFactory) {
        Long userId = validateAndConsume(oldRawToken);
        String newRawToken = newTokenFactory.apply(userId);
        save(userId, newRawToken);
        return new RotationResult(userId, newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.deleteByTokenHash(hash(rawToken));
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /** 매일 새벽 3시에 만료된 토큰 정리 */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "refreshTokenCleanupScheduler", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    @Transactional
    public void cleanExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}
