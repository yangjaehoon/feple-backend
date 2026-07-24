package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.dto.KakaoUserResponseDto;
import com.feple.feple_backend.auth.kakao.KakaoApiClient;
import com.feple.feple_backend.user.NicknameGenerator;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KakaoAuthService implements OAuthLoginService {

    private final KakaoApiClient kakaoApiClient;
    private final NicknameGenerator nicknameGenerator;
    private final OAuthUserRegistrationService registrationService;

    @Override
    public Mono<User> authenticate(String accessToken) {
        return kakaoApiClient.getMe(accessToken).map(this::registerOrFind);
    }

    private User registerOrFind(KakaoUserResponseDto kakaoUser) {
        var account = Optional.ofNullable(kakaoUser.getKakaoAccount())
                .orElseThrow(() -> new IllegalArgumentException("카카오 계정 정보가 없습니다."));

        Long kakaoId = kakaoUser.getId();
        if (kakaoId == null) throw new IllegalArgumentException("카카오 사용자 ID를 받을 수 없습니다.");
        String oauthId = kakaoId.toString();
        String email = account.getEmail();

        String rawNickname = Optional.ofNullable(account.getProfile())
                .map(KakaoUserResponseDto.Profile::getNickname)
                .filter(n -> !n.isBlank())
                .orElse("KakaoUser");
        String fallback = "Kakao" + oauthId.substring(0, Math.min(oauthId.length(), 4));

        String kakaoImageUrl = Optional.ofNullable(account.getProfile())
                .map(KakaoUserResponseDto.Profile::getProfile_image_url)
                .filter(url -> !url.isBlank())
                .orElse(null);

        return registrationService.registerOrFind(AuthProvider.KAKAO, oauthId,
                () -> nicknameGenerator.uniquify(nicknameGenerator.sanitize(rawNickname, fallback)),
                nickname -> User.builder()
                        .oauthId(oauthId)
                        .email(email)
                        .nickname(nickname)
                        .provider(AuthProvider.KAKAO)
                        .profileImageUrl(kakaoImageUrl)
                        .build());
    }
}
