package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.dto.KakaoUserResponseDto;
import com.feple.feple_backend.auth.kakao.KakaoApiClient;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class KakaoAuthService implements OAuthLoginService {

    private static final String DEFAULT_NICKNAME = "KakaoUser";
    private static final String FALLBACK_NICKNAME_PREFIX = "Kakao";
    private static final int FALLBACK_NICKNAME_ID_LENGTH = 4;

    private final KakaoApiClient kakaoApiClient;
    private final OAuthUserRegistrationService registrationService;

    @Override
    public Mono<User> authenticate(String accessToken) {
        // registerOrFind는 블로킹 JDBC(조회 + 닉네임 유일성 확인 + 저장)다. map으로 두면
        // WebClient 응답이 도착한 reactor-netty 이벤트 루프 스레드에서 그대로 실행돼,
        // 동시 로그인이나 커넥션 풀 대기 한 번으로 루프가 점유되고 같은 루프를 쓰는 다른
        // WebClient 호출까지 멈춘다 — FirebaseAuthService와 동일하게 boundedElastic으로
        // 오프로드한다.
        return kakaoApiClient.getMe(accessToken)
                .flatMap(kakaoUser -> Mono.fromCallable(() -> registerOrFind(kakaoUser))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private User registerOrFind(KakaoUserResponseDto kakaoUser) {
        var account = Optional.ofNullable(kakaoUser.getKakaoAccount())
                .orElseThrow(() -> new InvalidRequestException("카카오 계정 정보가 없습니다."));

        Long kakaoId = kakaoUser.getId();
        if (kakaoId == null) throw new InvalidRequestException("카카오 사용자 ID를 받을 수 없습니다.");
        String oauthId = kakaoId.toString();

        Optional<KakaoUserResponseDto.Profile> profile = Optional.ofNullable(account.getProfile());
        String rawNickname = profile.map(KakaoUserResponseDto.Profile::getNickname)
                .filter(n -> !n.isBlank())
                .orElse(DEFAULT_NICKNAME);
        String profileImageUrl = profile.map(KakaoUserResponseDto.Profile::getProfile_image_url)
                .filter(url -> !url.isBlank())
                .orElse(null);

        return registrationService.registerOrFind(new OAuthProfile(
                AuthProvider.KAKAO,
                oauthId,
                account.getEmail(),
                rawNickname,
                OAuthProfile.fallbackNickname(FALLBACK_NICKNAME_PREFIX, oauthId, FALLBACK_NICKNAME_ID_LENGTH),
                profileImageUrl));
    }
}
