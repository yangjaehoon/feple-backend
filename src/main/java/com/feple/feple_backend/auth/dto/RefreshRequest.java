package com.feple.feple_backend.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshRequest {
    private String refreshToken;

    public RefreshRequest() {
    }

}