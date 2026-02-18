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
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    public boolean isRefreshToken(String token) {
        try {
            String sub = Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return "refresh".equals(sub);
        } catch (Exception e) {
            return false;
        }
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + props.refreshTokenExpirationMs());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }


    public Long parseUserId(String token) {
        String sub = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.valueOf(sub);
    }
}