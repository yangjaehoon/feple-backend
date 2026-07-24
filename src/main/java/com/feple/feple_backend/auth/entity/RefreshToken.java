package com.feple.feple_backend.auth.entity;

import com.feple.feple_backend.user.entity.User;
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
        @Index(name = "idx_refresh_user_id", columnList = "user_id")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Long getUserId() {
        return user.getId();
    }

    /** 실제 토큰의 SHA-256 해시 값 (평문 저장 방지) */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static RefreshToken of(User user, String tokenHash, LocalDateTime expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.user = user;
        rt.tokenHash = tokenHash;
        rt.expiresAt = expiresAt;
        return rt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
