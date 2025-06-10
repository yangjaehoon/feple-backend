package com.feple.feple_backend.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

@Data
public class KakaoUserResponse {
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Data
    public static class KakaoAccount {

        private String email;
        /** 프로필 전체 동의 여부 */
        private Boolean profile_needs_agreement;
        /** 닉네임 동의 여부 */
        private Boolean profile_nickname_needs_agreement;
        /** 프로필 사진 동의 여부 */
        private Boolean profile_image_needs_agreement;
        /** 프로필 상세 */
        private Profile profile;
        /** 성별 */
        private String gender;

    }

    @Data
    public static class Profile {
        /** 썸네일 URL */
        private String thumbnail_image_url;
        /** 원본 프로필 사진 URL */
        private String profile_image_url;
        /** 기본 이미지 여부 */
        private Boolean is_default_image;

        private String nickname;
    }
}