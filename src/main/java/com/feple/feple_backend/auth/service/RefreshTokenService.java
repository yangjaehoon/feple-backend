package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.entity.RefreshToken;
import com.feple.feple_backend.auth.jwt.JwtProperties;
import com.feple.feple_backend.auth.repository.RefreshTokenRepository;
import com.feple.feple_backend.global.KoreaClock;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.global.exception.UserBannedException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    /**
     * 교체된 리프레시 토큰을 다시 받아주는 시간. 클라이언트는 갱신이 일시적으로 실패하면
     * 세션을 유지하고 다음 401에서 다시 갱신하므로, 사용자의 다음 조작까지 기다릴 수 있게
     * 분 단위로 둔다.
     */
    static final Duration ROTATION_GRACE = Duration.ofMinutes(5);

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

    /**
     * 새 로그인 — 사용자의 기존 토큰을 유예 중인 것까지 모두 끊고 새 토큰을 저장한다
     * (단일 기기 세션 정책).
     */
    @Transactional
    public void save(Long userId, String rawToken) {
        refreshTokenRepository.deleteByUserId(userId);
        insert(userId, rawToken);
    }

    private void insert(Long userId, String rawToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("토큰 저장 중 사용자를 찾을 수 없습니다."));
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.refreshTokenExpirationMs() / 1000);
        refreshTokenRepository.save(RefreshToken.of(user, hash(rawToken), expiresAt));
    }

    /**
     * 리프레시 토큰을 검증하고 교체 표시한 뒤 userId를 반환한다.
     *
     * <p>교체된 토큰도 {@link #ROTATION_GRACE} 동안은 다시 받아준다. 서버가 교체를 커밋한 뒤
     * 응답이 유실되면(현장 저신호의 수신 타임아웃 등) 클라이언트는 옛 토큰만 들고 있는데,
     * 즉시 무효화하면 다음 갱신이 400이 돼 강제 로그아웃된다. 유예 중 재요청에는 새 토큰을
     * 다시 발급하고, 그 사이 발급됐던(클라이언트가 못 받은) 토큰은 {@link #rotate}가 지운다.
     * 대가로 탈취된 토큰도 이 창 안에서는 재사용될 수 있다 — 창을 짧게 둔다.
     */
    @Transactional
    public Long validateAndConsume(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRequestException("유효하지 않은 리프레시 토큰입니다."));

        if (stored.isExpired()) {
            // 여기서 만료 행을 삭제하지 않는다 — 바로 아래 예외가 같은 트랜잭션을
            // 롤백시켜 삭제가 무효화되기 때문이다. 별도 빈으로 빼 REQUIRES_NEW를 걸어도
            // 그 트랜잭션 안에서 예외를 던지면 마찬가지라, 삭제만 수행하고 커밋한 뒤
            // 돌아오는 별도 메서드가 필요하다. 만료 토큰은 이미 사용할 수 없고 행 정리는
            // cleanExpiredTokens() 스케줄러가 맡으므로 그만한 값어치가 없다.
            throw new InvalidRequestException("만료된 리프레시 토큰입니다. 다시 로그인해주세요.");
        }

        // 정지 계정은 갱신을 거부한다 — 예외로 트랜잭션이 롤백돼 토큰은 소비되지 않는다.
        if (stored.isOwnerBanned()) {
            throw new UserBannedException();
        }

        if (stored.isRotated() && !stored.isWithinRotationGrace(ROTATION_GRACE)) {
            throw new InvalidRequestException("유효하지 않은 리프레시 토큰입니다.");
        }
        refreshTokenRepository.markRotated(tokenHash, LocalDateTime.now());
        return stored.getUserId();
    }

    public record RotationResult(Long userId, String newRefreshToken) {}

    /**
     * 기존 리프레시 토큰을 검증·교체 표시하고 새 토큰을 발급·저장하는 과정을 하나의 트랜잭션으로
     * 묶는다 — 중간에 실패하면 전체 롤백되어 기존 토큰이 그대로 보존된다.
     */
    @Transactional
    public RotationResult rotate(String oldRawToken, Function<Long, String> newTokenFactory) {
        Long userId = validateAndConsume(oldRawToken);
        String newRawToken = newTokenFactory.apply(userId);
        refreshTokenRepository.deleteSupersededByUserId(userId, graceCutoff());
        insert(userId, newRawToken);
        return new RotationResult(userId, newRawToken);
    }

    /**
     * 로그아웃. 아직 쓸 수 있는 토큰이면 유예 중인 직전 토큰까지 사용자의 토큰을 모두 끊는다
     * (남기면 로그아웃 후에도 갱신 가능). 이미 쓸 수 없는 토큰(만료·유예 경과)이면 그 행만
     * 지운다 — {@code /auth/logout}은 인증 없이 열려 있어, 오래된 토큰을 쥔 누군가가 현재
     * 세션을 끊을 수 있으면 안 된다.
     */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (isUsable(token)) {
                refreshTokenRepository.deleteByUserId(token.getUserId());
            } else {
                refreshTokenRepository.delete(token);
            }
        });
    }

    private static boolean isUsable(RefreshToken token) {
        return !token.isExpired()
                && (!token.isRotated() || token.isWithinRotationGrace(ROTATION_GRACE));
    }

    @Transactional
    public void revokeAll(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /** 매일 새벽 3시에 만료된 토큰 정리 */
    @Scheduled(cron = "0 0 3 * * *", zone = KoreaClock.ZONE_ID)
    @SchedulerLock(name = "refreshTokenCleanupScheduler", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    @Transactional
    public void cleanExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        refreshTokenRepository.deleteRotatedBefore(graceCutoff());
    }

    private static LocalDateTime graceCutoff() {
        return LocalDateTime.now().minus(ROTATION_GRACE);
    }
}
