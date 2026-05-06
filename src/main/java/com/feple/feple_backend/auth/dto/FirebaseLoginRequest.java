package com.feple.feple_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FirebaseLoginRequest {

    @NotBlank(message = "Firebase ID 토큰이 필요합니다.")
    private String idToken;

    /** 회원가입 시 사용자가 입력한 닉네임 (로그인 시에는 null) */
    private String nickname;
}
