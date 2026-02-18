package com.feple.feple_backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshRequest {
    private String refreshToken;

    public RefreshRequest() {}

}