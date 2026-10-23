package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;

/**
 * OAuth 공급자에서 받아온 사용자 정보 — 기존 유저 조회 키와 신규 가입에 필요한 값을 함께 담는다.
 *
 * @param rawNickname      닉네임 후보(공급자가 준 이름). 정제·유일화는 가입 시점에 한다.
 * @param fallbackNickname 후보가 금칙어·형식 위반 등으로 쓸 수 없을 때의 대체 닉네임
 */
public record OAuthProfile(
        AuthProvider provider,
        String oauthId,
        String email,
        String rawNickname,
        String fallbackNickname,
        String profileImageUrl
) {

    /** 공급자 접두어 + oauthId 앞부분으로 대체 닉네임을 만든다 (예: {@code Kakao1234}). */
    public static String fallbackNickname(String prefix, String oauthId, int idLength) {
        return prefix + oauthId.substring(0, Math.min(oauthId.length(), idLength));
    }

    User toUser(String nickname) {
        return User.builder()
                .provider(provider)
                .oauthId(oauthId)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();
    }
}
