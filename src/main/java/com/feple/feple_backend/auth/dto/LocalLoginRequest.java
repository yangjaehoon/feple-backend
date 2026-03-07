package com.feple.feple_backend.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LocalLoginRequest {
    private String email;
    private String password;
}
