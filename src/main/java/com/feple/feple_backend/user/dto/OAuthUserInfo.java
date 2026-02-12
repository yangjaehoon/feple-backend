package com.feple.feple_backend.user.dto;

import com.feple.feple_backend.user.domain.AuthProvider;
import lombok.Getter;

@Getter
public class OAuthUserInfo {
    private String email;
    private String nickname;
    private String oauthId;
    private String profileImageUrl;
    private AuthProvider provider;
}
