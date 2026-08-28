package com.feple.feple_backend.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters-long-for-hs256";
    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final long ACCESS_EXP_MS = 1000L * 60 * 15;
    private static final long REFRESH_EXP_MS = 1000L * 60 * 60 * 24 * 14;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(SECRET, ACCESS_EXP_MS, REFRESH_EXP_MS);
        jwtProvider = new JwtProvider(props, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void 액세스_토큰_생성후_파싱하면_userId_반환() {
        String token = jwtProvider.createAccessToken(42L);

        assertThat(jwtProvider.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void 액세스_토큰은_isRefreshToken_false() {
        String token = jwtProvider.createAccessToken(1L);

        assertThat(jwtProvider.isRefreshToken(token)).isFalse();
    }

    @Test
    void 리프레시_토큰은_isRefreshToken_true() {
        String token = jwtProvider.createRefreshToken(1L);

        assertThat(jwtProvider.isRefreshToken(token)).isTrue();
    }

    @Test
    void 리프레시_토큰으로_parseUserId_호출시_예외() {
        String refreshToken = jwtProvider.createRefreshToken(1L);

        assertThatThrownBy(() -> jwtProvider.parseUserId(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("액세스 토큰이 아닙니다.");
    }

    @Test
    void 만료된_액세스_토큰_파싱시_예외() {
        JwtProperties props = new JwtProperties(SECRET, ACCESS_EXP_MS, REFRESH_EXP_MS);
        // 발급 시점을 만료 기간보다 더 과거로 고정하면, FIXED_NOW 기준 검증 시 이미 만료다.
        JwtProvider issuedInPast = new JwtProvider(
                props, Clock.fixed(FIXED_NOW.minusMillis(ACCESS_EXP_MS * 2), ZoneOffset.UTC));
        String expiredToken = issuedInPast.createAccessToken(1L);

        assertThatThrownBy(() -> jwtProvider.parseUserId(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void 위조된_토큰은_isRefreshToken_false로_처리() {
        String tampered = jwtProvider.createRefreshToken(1L) + "tampered";

        assertThat(jwtProvider.isRefreshToken(tampered)).isFalse();
    }

    @Test
    void 다른_비밀키로_서명된_토큰은_파싱시_예외() {
        JwtProperties otherProps = new JwtProperties(
                "another-secret-key-must-be-at-least-32-characters-long-too", ACCESS_EXP_MS, REFRESH_EXP_MS);
        JwtProvider otherProvider = new JwtProvider(otherProps, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
        String tokenFromOtherKey = otherProvider.createAccessToken(1L);

        assertThatThrownBy(() -> jwtProvider.parseUserId(tokenFromOtherKey))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
