package com.feple.feple_backend.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "tokenHash"),
        @Index(name = "idx_refresh_user_id", columnList = "userId")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 실제 토큰의 SHA-256 해시 값 (평문 저장 방지) */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static RefreshToken of(Long userId, String tokenHash, LocalDateTime expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.userId = userId;
        rt.tokenHash = tokenHash;
        rt.expiresAt = expiresAt;
        return rt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
