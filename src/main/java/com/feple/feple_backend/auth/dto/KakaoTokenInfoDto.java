package com.feple.feple_backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 카카오 {@code /v1/user/access_token_info} 응답 중 토큰 발급 앱 확인에 필요한 필드만. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenInfoDto(@JsonProperty("app_id") Long appId) {
}
