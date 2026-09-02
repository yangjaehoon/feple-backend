package com.feple.feple_backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    // 나이 확인용 생년월일. NULL이면 아직 확인 전(로그인은 됐으나 게이트 미통과) —
    // JwtAuthenticationFilter가 GET /users/me 외의 요청을 차단한다. 만 14세 미만으로
    // 확인되면 계정이 즉시 소프트 삭제되므로 이 컬럼에 미달 생년월일이 저장되는 일은 없다.
    @Column(name = "birth_date")
    private LocalDate birthDate;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "withdrawal_reason", length = 30)
    private WithdrawalReason withdrawalReason;

    @Column(name = "withdrawal_detail", length = 300)
    private String withdrawalDetail;

    @Column(nullable = false)
    @Builder.Default
    private int point = 0;

    @Column(name = "nickname_changed_at")
    private LocalDateTime nicknameChangedAt;

    public static final int NICKNAME_COOLDOWN_DAYS = 90;

    public boolean isAdmin() { return UserStatusPolicy.isAdmin(role); }
    public boolean isArtist() { return UserStatusPolicy.isArtist(role); }
    public boolean isDeleted() { return deletedAt != null; }

    /**
     * 나이 확인이 필요한 활성 일반 유저인지 — 생년월일 미입력 + 미탈퇴 + 일반 회원.
     * 관리자·아티스트 계정은 가입 흐름을 타지 않으므로 대상에서 제외한다.
     */
    public boolean needsAgeVerification() {
        return birthDate == null && deletedAt == null && role == UserRole.USER;
    }

    public void recordBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

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

    public void softDelete(WithdrawalReason reason, String detail) {
        this.deletedAt = LocalDateTime.now();
        this.nickname = "(탈퇴한 사용자)";
        // oauthId는 유지 — 동일 계정으로 재가입 시 차단하기 위함
        this.email = null;
        this.bio = null;
        this.profileImageUrl = null;
        this.withdrawalReason = reason;
        this.withdrawalDetail = (detail != null && !detail.isBlank()) ? detail.strip() : null;
    }

}
