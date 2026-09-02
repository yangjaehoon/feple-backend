package com.feple.feple_backend.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.entity.UserStatusPolicy;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private UserRole role;
    private String bio;
    private String level;
    private LocalDateTime nicknameChangedAt;
    /** 나이 확인이 아직 필요한 계정이면 true, 아니면 null(응답에서 생략). 클라이언트는 이 값으로 나이 확인 화면을 띄운다. */
    private Boolean ageVerificationRequired;
    /** 관리자 페이지에서만 사용. 공개 API 응답에는 포함하지 않음. */
    private String email;
    /** 관리자 페이지에서만 사용. */
    private LocalDateTime createdAt;

    /** 관리자 페이지에서만 사용. null이면 미정지. */
    private LocalDateTime bannedUntil;
    /** 관리자 페이지에서만 사용. */
    private String banReason;
    /** 관리자 페이지에서만 사용. */
    private String bannedBy;

    /** 관리자 페이지에서만 사용. null이면 정상 계정. */
    private LocalDateTime deletedAt;
    /** 관리자 페이지에서만 사용. */
    private AuthProvider provider;
    /** 관리자 페이지에서만 사용. */
    private Integer point;

    public String getRoleDisplayName() { return role != null ? role.getDisplayName() : ""; }
    public boolean isAdmin() { return UserStatusPolicy.isAdmin(role); }
    public boolean isArtist() { return UserStatusPolicy.isArtist(role); }
    public boolean isRegularUser() { return !isAdmin() && !isArtist(); }
    public boolean isBanned() { return UserStatusPolicy.isBanned(bannedUntil); }
    public boolean isPermanentBan() { return UserStatusPolicy.isPermanentBan(bannedUntil); }
}
