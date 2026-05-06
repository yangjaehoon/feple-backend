package com.feple.feple_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshRequest {
    @NotBlank(message = "리프레시 토큰은 필수입니다.")
    private String refreshToken;

    public RefreshRequest() {
    }

}