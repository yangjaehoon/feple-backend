package com.feple.feple_backend.dto.user;

import com.feple.feple_backend.domain.user.AuthProvider;
import lombok.Getter;

@Getter
public class UserRequestDto {
    private String email;
    private String nickname;
    private String oauthId;
    private String profileImageUrl;
    private AuthProvider provider;
}
