package com.feple.feple_backend.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;


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
// @ManyToOne으로 User를 참조하는 연관관계(Comment.user 등)를 지연 로딩할 때, 클래스 레벨
// BatchSize로 여러 프록시를 한 번에 배치 조회해 N+1을 방지한다 — Hibernate 6는 to-one
// 연관관계 필드에 프로퍼티 레벨 @BatchSize를 허용하지 않으므로(컬렉션 전용) 대상 엔티티에 붙여야 한다.
@BatchSize(size = 20)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 닉네임 유일성은 활성 유저 사이에서만 요구된다(UserRepository.existsByNickname 등이
    // deletedAt IS NULL로 스코핑) — 탈퇴 유저는 전부 같은 표시 문자열을 쓰므로 DB 유니크
    // 제약을 걸면 안 된다(V62 마이그레이션에서 idx_user_nickname의 UNIQUE를 제거).
    @Column
    private String nickname;

    @Column(nullable = false)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    @Column(length = 500)
    private String profileImageUrl;

    @Column(nullable = true)
    private String email;

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

    @Column(nullable = false)
    @Builder.Default
    private int point = 0;

    @Column(name = "nickname_changed_at")
    private LocalDateTime nicknameChangedAt;

    public static final int NICKNAME_COOLDOWN_DAYS = 90;

    public boolean isAdmin() { return UserStatusPolicy.isAdmin(role); }
    public boolean isArtist() { return UserStatusPolicy.isArtist(role); }
    public boolean isDeleted() { return deletedAt != null; }

    public boolean isBanned() {
        return UserStatusPolicy.isBanned(bannedUntil);
    }

    public void ban(int days, String reason, String bannedBy) {
        this.bannedUntil = (days <= 0)
                ? UserStatusPolicy.permanentBanUntil()
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

    public boolean canChangeNickname() {
        return nicknameChangedAt == null || LocalDateTime.now().isAfter(nextNicknameChangeAt());
    }

    public LocalDateTime nextNicknameChangeAt() {
        return nicknameChangedAt == null ? null : nicknameChangedAt.plusDays(NICKNAME_COOLDOWN_DAYS);
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
        this.nicknameChangedAt = LocalDateTime.now();
    }

    public void changeProfileImage(String imageUrl) {
        this.profileImageUrl = imageUrl;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public UserLevel getLevel() {
        return UserLevel.of(this.point);
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
