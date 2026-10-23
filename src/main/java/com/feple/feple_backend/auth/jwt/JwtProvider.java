package com.feple.feple_backend.auth.jwt;

import com.feple.feple_backend.global.exception.InvalidRequestException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public final class JwtProvider {

    private final JwtProperties props;
    private final SecretKey key;
    private final Clock clock;

    public JwtProvider(JwtProperties props, Clock clock) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.clock = clock;
    }

    public String createAccessToken(Long userId) {
        return issueToken(userId, props.accessTokenExpirationMs(), JwtConstants.TOKEN_TYPE_ACCESS);
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = parseClaims(token).get(JwtConstants.CLAIM_TYPE, String.class);
            return JwtConstants.TOKEN_TYPE_REFRESH.equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public String createRefreshToken(Long userId) {
        return issueToken(userId, props.refreshTokenExpirationMs(), JwtConstants.TOKEN_TYPE_REFRESH);
    }

    private String issueToken(Long userId, long expirationMs, String tokenType) {
        Instant now = clock.instant();
        Date issuedAt = Date.from(now);
        Date expiration = Date.from(now.plusMillis(expirationMs));

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(JwtConstants.CLAIM_TYPE, tokenType)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /** access 토큰인 경우에만 userId 반환, 아니면 예외 */
    public Long parseUserId(String token) {
        Claims payload = parseClaims(token);

        String type = payload.get(JwtConstants.CLAIM_TYPE, String.class);
        if (!JwtConstants.TOKEN_TYPE_ACCESS.equals(type)) {
            throw new InvalidRequestException("액세스 토큰이 아닙니다.");
        }

        return Long.valueOf(payload.getSubject());
    }

    /** 서명·만료를 검증하고 클레임을 꺼낸다. 실패하면 jjwt 예외를 그대로 던진다. */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clock(jwtClock())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // jjwt는 만료 검증에 자체 시계(기본 System)를 쓴다 — 주입된 Clock을 넘겨 검증 시각도 제어 가능하게 한다.
    private io.jsonwebtoken.Clock jwtClock() {
        return () -> Date.from(clock.instant());
    }
}
