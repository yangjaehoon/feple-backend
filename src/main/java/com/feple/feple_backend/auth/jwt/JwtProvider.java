package com.feple.feple_backend.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties props;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.accessTokenExpirationMs());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "access")
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("type", String.class);
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.refreshTokenExpirationMs());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }


    /** access 토큰인 경우에만 userId 반환, 아니면 예외 */
    public Long parseUserId(String token) {
        var payload = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = payload.get("type", String.class);
        if (!"access".equals(type)) {
            throw new IllegalArgumentException("액세스 토큰이 아닙니다.");
        }

        return Long.valueOf(payload.getSubject());
    }
}