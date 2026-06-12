package com.feple.feple_backend.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private UserRole role;
    private String bio;
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

    public boolean isAdmin() { return role == UserRole.ADMIN; }
    public boolean isArtist() { return role == UserRole.ARTIST; }
    public boolean isRegularUser() { return !isAdmin() && !isArtist(); }
    public boolean isBanned() { return bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now()); }
    public boolean isPermanentBan() { return bannedUntil != null && bannedUntil.getYear() >= 9999; }
}