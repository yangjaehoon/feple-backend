package com.feple.feple_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.auth.entity.RefreshToken;
import com.feple.feple_backend.auth.jwt.JwtProperties;
import com.feple.feple_backend.auth.repository.RefreshTokenRepository;
import com.feple.feple_backend.global.exception.UserBannedException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock UserRepository userRepository;

    private RefreshTokenService refreshTokenService;

    // 24시간 만료, 레코드 직접 생성으로 mock 불필요
    private static final JwtProperties JWT_PROPS =
            new JwtProperties("test-secret-key-long-enough", 3_600_000L, 86_400_000L);

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, JWT_PROPS, userRepository);
    }

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
    }

    private RefreshToken validToken(User user, String rawToken) {
        String hash = refreshTokenService.hash(rawToken);
        return RefreshToken.of(user, hash, LocalDateTime.now().plusDays(1));
    }

    /** {@code rotatedAgo} 전에 교체 표시된 토큰. */
    private RefreshToken rotatedToken(User user, String rawToken, Duration rotatedAgo) {
        RefreshToken token = validToken(user, rawToken);
        ReflectionTestUtils.setField(token, "rotatedAt", LocalDateTime.now().minus(rotatedAgo));
        return token;
    }

    private RefreshToken expiredToken(User user, String rawToken) {
        String hash = refreshTokenService.hash(rawToken);
        return RefreshToken.of(user, hash, LocalDateTime.now().minusHours(1));
    }

    // ── hash ─────────────────────────────────────────────────────────

    @Test
    void 동일_입력이면_동일_해시_반환() {
        String h1 = refreshTokenService.hash("token-abc");
        String h2 = refreshTokenService.hash("token-abc");

        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void 다른_입력이면_다른_해시_반환() {
        String h1 = refreshTokenService.hash("token-abc");
        String h2 = refreshTokenService.hash("token-xyz");

        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void 해시는_64자_16진수_문자열() {
        String hash = refreshTokenService.hash("any-token");

        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    // ── save ─────────────────────────────────────────────────────────

    @Test
    void 저장시_기존_토큰_삭제_후_새_토큰_저장() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        refreshTokenService.save(1L, "raw-token");

        verify(refreshTokenRepository).deleteByUserId(1L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void 존재하지_않는_사용자로_저장시_예외() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.save(99L, "raw-token"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── validateAndConsume ───────────────────────────────────────────

    @Test
    void 유효한_토큰_소비시_userId_반환() {
        User user = user(1L);
        String raw = "valid-token";
        RefreshToken token = validToken(user, raw);
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .willReturn(Optional.of(token));

        Long result = refreshTokenService.validateAndConsume(raw);

        assertThat(result).isEqualTo(1L);
        verify(refreshTokenRepository).markRotated(eq(refreshTokenService.hash(raw)), any());
    }

    @Test
    void 유예시간_안에_교체된_토큰이면_다시_받아준다() {
        User user = user(1L);
        String raw = "just-rotated-token";
        RefreshToken token = rotatedToken(user, raw, Duration.ofSeconds(30));
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .willReturn(Optional.of(token));

        assertThat(refreshTokenService.validateAndConsume(raw)).isEqualTo(1L);
    }

    @Test
    void 유예시간이_지난_교체된_토큰이면_예외() {
        User user = user(1L);
        String raw = "old-rotated-token";
        RefreshToken token = rotatedToken(user, raw,
                RefreshTokenService.ROTATION_GRACE.plusSeconds(1));
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .willReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateAndConsume(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은");
        verify(refreshTokenRepository, never()).markRotated(any(), any());
    }

    @Test
    void 존재하지_않는_토큰_소비시_예외() {
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndConsume("unknown-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은");
    }

    @Test
    void 정지된_유저의_토큰이면_소비하지_않고_UserBannedException() {
        User banned = User.builder().id(5L).nickname("user5").oauthId("o5").role(UserRole.USER)
                .bannedUntil(LocalDateTime.now().plusDays(3)).build();
        String raw = "banned-token";
        RefreshToken token = validToken(banned, raw);
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .willReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateAndConsume(raw))
                .isInstanceOf(UserBannedException.class);

        verify(refreshTokenRepository, never()).markRotated(any(), any());
    }

    // 만료 행 삭제를 여기서 하지 않는 이유: 곧바로 던지는 예외가 같은 트랜잭션을
    // 롤백시켜 삭제가 무효화된다(rotate()의 자기 호출이라 전파 설정으로도 분리 불가).
    // 정리는 cleanExpiredTokens() 스케줄러가 담당한다.
    @Test
    void 만료된_토큰_소비시_삭제하지_않고_예외() {
        User user = user(1L);
        String raw = "expired-token";
        RefreshToken token = expiredToken(user, raw);
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .willReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateAndConsume(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료");

        verify(refreshTokenRepository, never()).markRotated(any(), any());
    }

    // ── rotate ───────────────────────────────────────────────────────

    @Test
    void 로테이트시_기존토큰_소비하고_새토큰_발급저장() {
        User user = user(1L);
        String rawOld = "old-token";
        RefreshToken token = validToken(user, rawOld);
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(rawOld)))
                .willReturn(Optional.of(token));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        RefreshTokenService.RotationResult result =
                refreshTokenService.rotate(rawOld, userId -> "new-token-for-" + userId);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.newRefreshToken()).isEqualTo("new-token-for-1");
        verify(refreshTokenRepository).markRotated(eq(refreshTokenService.hash(rawOld)), any());
        // 유예 중인 직전 토큰은 남기고 나머지만 정리한다 — 로그인 경로의 전체 삭제와 다르다.
        verify(refreshTokenRepository).deleteSupersededByUserId(eq(1L), any());
        verify(refreshTokenRepository, never()).deleteByUserId(any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void 로테이트시_기존토큰_유효하지않으면_새토큰_발급안됨() {
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown-token", userId -> "new-token"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    // ── revoke ───────────────────────────────────────────────────────

    @Test
    void 토큰_revoke시_유예중인_직전_토큰까지_사용자_토큰_전부_삭제() {
        User user = user(1L);
        String raw = "revoke-token";
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .willReturn(Optional.of(validToken(user, raw)));

        refreshTokenService.revoke(raw);

        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void 이미_쓸수없는_토큰으로_revoke하면_그_행만_삭제하고_현재_세션은_유지() {
        User user = user(1L);
        String raw = "stale-rotated-token";
        RefreshToken stale = rotatedToken(user, raw,
                RefreshTokenService.ROTATION_GRACE.plusMinutes(1));
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .willReturn(Optional.of(stale));

        refreshTokenService.revoke(raw);

        verify(refreshTokenRepository).delete(stale);
        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    @Test
    void 존재하지_않는_토큰_revoke시_예외_없이_무시() {
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.empty());

        refreshTokenService.revoke("unknown-token");

        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }

    // ── revokeAll / cleanExpiredTokens ───────────────────────────────

    @Test
    void revokeAll_호출시_deleteByUserId_실행() {
        refreshTokenService.revokeAll(1L);

        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void cleanExpiredTokens_호출시_deleteExpiredTokens_실행() {
        refreshTokenService.cleanExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
        verify(refreshTokenRepository).deleteRotatedBefore(any(LocalDateTime.class));
    }
}
