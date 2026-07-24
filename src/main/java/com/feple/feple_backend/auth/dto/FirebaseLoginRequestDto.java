package com.feple.feple_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FirebaseLoginRequestDto {

    @NotBlank(message = "Firebase ID 토큰이 필요합니다.")
    private String idToken;
}
