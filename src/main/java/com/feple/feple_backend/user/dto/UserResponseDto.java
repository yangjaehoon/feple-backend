package com.feple.feple_backend.user.dto;

import com.feple.feple_backend.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDto {
    private Long id;
    private String nickname;
    private String profileImageUrl;
    private UserRole role;
    /** 관리자 페이지에서만 사용. 공개 API 응답에는 포함하지 않음. */
    private String email;
}