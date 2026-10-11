package com.feple.feple_backend.auth.kakao;

import com.feple.feple_backend.auth.dto.KakaoTokenInfoDto;
import com.feple.feple_backend.auth.dto.KakaoUserResponseDto;
import com.feple.feple_backend.auth.jwt.JwtConstants;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KakaoApiClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoApiClient.class);

    private static final String TOKEN_INFO_URI = "https://kapi.kakao.com/v1/user/access_token_info";
    private static final String USER_ME_URI = "https://kapi.kakao.com/v2/user/me";
    private static final String LOGIN_FAILED_MESSAGE = "카카오 로그인에 실패했습니다. 다시 시도해주세요.";

    private final WebClient kakaoWebClient;
    private final KakaoAuthProperties properties;

    /**
     * 토큰이 이 앱에서 발급됐는지 먼저 확인한 뒤 사용자 정보를 조회한다.
     * {@code /v2/user/me}는 "유효한 카카오 토큰인가"만 보므로, 앱 확인 없이 쓰면 다른 카카오
     * 연동 앱이 받아둔 사용자 토큰으로 그 사용자 행세를 할 수 있다(토큰 치환 공격).
     */
    public Mono<KakaoUserResponseDto> getMe(String kakaoAccessToken) {
        return verifyIssuedForThisApp(kakaoAccessToken)
                .then(Mono.defer(() -> fetchMe(kakaoAccessToken)));
    }

    private Mono<Void> verifyIssuedForThisApp(String kakaoAccessToken) {
        Long expectedAppId = properties.appId();
        return kakaoWebClient.get()
                .uri(TOKEN_INFO_URI)
                .header(HttpHeaders.AUTHORIZATION, JwtConstants.BEARER_PREFIX + kakaoAccessToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, KakaoApiClient::toLoginFailure)
                .bodyToMono(KakaoTokenInfoDto.class)
                .flatMap(info -> {
                    if (expectedAppId.equals(info.appId())) {
                        return Mono.empty();
                    }
                    log.warn("[Kakao Auth] 다른 앱에서 발급된 토큰 거부 - app_id: {}", info.appId());
                    return Mono.error(new InvalidRequestException(LOGIN_FAILED_MESSAGE));
                });
    }

    private Mono<KakaoUserResponseDto> fetchMe(String kakaoAccessToken) {
        return kakaoWebClient.post()
                .uri(USER_ME_URI)
                .header(HttpHeaders.AUTHORIZATION, JwtConstants.BEARER_PREFIX + kakaoAccessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                // 4xx(토큰 만료·무효 등)는 클라이언트가 재로그인해야 하는 상황 — 그대로 두면
                // WebClientResponseException이 catch-all(500)으로 떨어진다. Firebase 경로와 동일하게
                // 400으로 매핑되도록 InvalidRequestException으로 변환한다. 5xx·연결 실패는 상위에서
                // 외부 서비스 오류(502)로 처리하도록 그대로 전파한다.
                .onStatus(HttpStatusCode::is4xxClientError, KakaoApiClient::toLoginFailure)
                .bodyToMono(KakaoUserResponseDto.class);
    }

    private static Mono<InvalidRequestException> toLoginFailure(ClientResponse response) {
        return response.createException()
                .map(ignored -> new InvalidRequestException(LOGIN_FAILED_MESSAGE));
    }
}
