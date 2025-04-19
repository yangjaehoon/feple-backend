package com.feple.feple_backend.dto.user;

import lombok.Data;

@Data
public class KakaoUserResponse {
    private Long id;
    private KakaoAccount kakao_account;

    @Data
    public static class KakaoAccount {
        private Profile profile;
        private String email;
        private String name;
        private String age_range;
        private String birthday;
        private String gender;

        @Data
        public static class Profile {
            private String nickname;
            private String profile_image_url;
            private String thumbnail_image_url;
        }
    }
}