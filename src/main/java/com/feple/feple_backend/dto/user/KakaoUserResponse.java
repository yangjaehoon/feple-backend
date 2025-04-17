package com.feple.feple_backend.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoUserResponse {

    private Long id;
    private KakaoAccount kakaoAccount;

    @Getter
    @Setter
    public static class KakaoAccount {
        private Profile profile;
        private String email;

        @Getter
        @Setter
        public static class Profile {
            private String nickname;
            private String profileImgUrl;
        }

    }
}
