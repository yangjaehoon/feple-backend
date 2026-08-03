package com.feple.feple_backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KakaoUserResponseDto {
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Data
    public static class KakaoAccount {

        private String email;
        /** 프로필 상세 */
        private Profile profile;
    }

    @Data
    public static class Profile {
        /** 원본 프로필 사진 URL */
        private String profile_image_url;

        private String nickname;
    }
}
