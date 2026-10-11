package com.feple.feple_backend.auth.entity;

import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_user_id", columnList = "user_id"),
        @Index(name = "idx_refresh_tokens_rotated_at", columnList = "rotated_at")
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

    public boolean isOwnerBanned() {
        return user.isBanned();
    }

    /** 실제 토큰의 SHA-256 해시 값 (평문 저장 방지) */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** 새 토큰으로 교체된 시각. null이면 현재 유효한 토큰이다(유예 시간 판정용). */
    private LocalDateTime rotatedAt;

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

    public boolean isRotated() {
        return rotatedAt != null;
    }

    /** 교체된 지 {@code grace} 이내인지 — 교체 응답을 못 받은 클라이언트의 재요청을 받아줄 창. */
    public boolean isWithinRotationGrace(Duration grace) {
        return rotatedAt != null && rotatedAt.isAfter(LocalDateTime.now().minus(grace));
    }
}
