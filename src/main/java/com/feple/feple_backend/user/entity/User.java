package com.feple.feple_backend.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "oauth_id"})
        },
        indexes = {
                @Index(name = "idx_users_created_at", columnList = "created_at"),
                @Index(name = "idx_users_deleted_at", columnList = "deleted_at")
        }
        )
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;

    @Column(nullable = false)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String profileImageUrl;

    @Column(nullable = true)
    private String email;

    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(length = 150)
    private String bio;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime bannedUntil;

    @Column(name = "ban_reason", length = 300)
    private String banReason;

    @Column(name = "banned_by", length = 100)
    private String bannedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isAdmin() { return role == UserRole.ADMIN; }
    public boolean isArtist() { return role == UserRole.ARTIST; }
    public boolean isDeleted() { return deletedAt != null; }

    public boolean isBanned() {
        return bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now());
    }

    public void ban(int days, String reason, String bannedBy) {
        this.bannedUntil = (days <= 0)
                ? LocalDateTime.of(9999, 12, 31, 23, 59, 59)
                : LocalDateTime.now().plusDays(days);
        this.banReason = (reason != null && !reason.isBlank()) ? reason.strip() : null;
        this.bannedBy = bannedBy;
    }

    public void unban() {
        this.bannedUntil = null;
        this.banReason = null;
        this.bannedBy = null;
    }

    public void changeRole(UserRole newRole) {
        this.role = newRole;
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void changeProfileImage(String imageUrl) {
        this.profileImageUrl = imageUrl;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.nickname = "(탈퇴한 사용자)";
        // oauthId는 유지 — 동일 계정으로 재가입 시 차단하기 위함
        this.email = null;
        this.bio = null;
        this.profileImageUrl = null;
    }

}
