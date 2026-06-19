package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.dto.KakaoUserResponse;
import com.feple.feple_backend.auth.kakao.KakaoApiClient;
import com.feple.feple_backend.user.NicknameGenerator;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class KakaoAuthService implements OAuthLoginService {

    private final KakaoApiClient kakaoApiClient;
    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;

    @Override
    public Mono<User> authenticate(String accessToken) {
        return kakaoApiClient.getMe(accessToken).map(this::registerOrFind);
    }

    private User registerOrFind(KakaoUserResponse kakaoUser) {
        var account = Optional.ofNullable(kakaoUser.getKakaoAccount())
                .orElseThrow(() -> new IllegalArgumentException("카카오 계정 정보가 없습니다."));

        Long kakaoId = kakaoUser.getId();
        if (kakaoId == null) throw new IllegalArgumentException("카카오 사용자 ID를 받을 수 없습니다.");
        String oauthId = kakaoId.toString();
        String email = account.getEmail();

        return userRepository.findByOauthId(oauthId).map(user -> {
            if (user.isDeleted()) {
                throw new IllegalArgumentException("탈퇴 처리된 계정입니다. 동일한 계정으로 재가입할 수 없습니다.");
            }
            return user;
        }).orElseGet(() -> {
            String rawNickname = Optional.ofNullable(account.getProfile())
                    .map(KakaoUserResponse.Profile::getNickname)
                    .filter(n -> !n.isBlank())
                    .orElse("KakaoUser");
            String fallback = "Kakao" + oauthId.substring(0, Math.min(oauthId.length(), 4));
            String nickname = nicknameGenerator.uniquify(nicknameGenerator.sanitize(rawNickname, fallback));

            String kakaoImageUrl = Optional.ofNullable(account.getProfile())
                    .map(KakaoUserResponse.Profile::getProfile_image_url)
                    .filter(url -> !url.isBlank())
                    .orElse(null);
            return userRepository.save(User.builder()
                    .oauthId(oauthId)
                    .email(email)
                    .nickname(nickname)
                    .provider(AuthProvider.KAKAO)
                    .profileImageUrl(kakaoImageUrl)
                    .build());
        });
    }
}
