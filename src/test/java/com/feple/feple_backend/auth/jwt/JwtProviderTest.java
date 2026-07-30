package com.feple.feple_backend.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-characters-long-for-hs256";

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(SECRET, 1000 * 60 * 15, 1000 * 60 * 60 * 24 * 14);
        jwtProvider = new JwtProvider(props);
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
        JwtProperties expiredProps = new JwtProperties(SECRET, -1000, 1000 * 60 * 60 * 24 * 14);
        JwtProvider expiredTokenProvider = new JwtProvider(expiredProps);
        String expiredToken = expiredTokenProvider.createAccessToken(1L);

        assertThatThrownBy(() -> expiredTokenProvider.parseUserId(expiredToken))
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
                "another-secret-key-must-be-at-least-32-characters-long-too", 1000 * 60 * 15, 1000 * 60 * 60 * 24 * 14);
        JwtProvider otherProvider = new JwtProvider(otherProps);
        String tokenFromOtherKey = otherProvider.createAccessToken(1L);

        assertThatThrownBy(() -> jwtProvider.parseUserId(tokenFromOtherKey))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
