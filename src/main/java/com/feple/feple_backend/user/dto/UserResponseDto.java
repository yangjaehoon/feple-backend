package com.feple.feple_backend.user.dto;

import com.feple.feple_backend.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
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

    public boolean isAdmin() { return role == UserRole.ADMIN; }
    public boolean isArtist() { return role == UserRole.ARTIST; }
}