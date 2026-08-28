package com.feple.feple_backend.auth.kakao;

import com.feple.feple_backend.auth.dto.KakaoUserResponseDto;
import com.feple.feple_backend.auth.jwt.JwtConstants;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KakaoApiClient {

    private final WebClient kakaoWebClient;

    public Mono<KakaoUserResponseDto> getMe(String kakaoAccessToken) {
        return kakaoWebClient.post()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, JwtConstants.BEARER_PREFIX + kakaoAccessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                // 4xx(토큰 만료·무효 등)는 클라이언트가 재로그인해야 하는 상황 — 그대로 두면
                // WebClientResponseException이 catch-all(500)으로 떨어진다. Firebase 경로와 동일하게
                // 400으로 매핑되도록 InvalidRequestException으로 변환한다. 5xx·연결 실패는 상위에서
                // 외부 서비스 오류(502)로 처리하도록 그대로 전파한다.
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> response.createException()
                                .map(ignored -> new InvalidRequestException("카카오 로그인에 실패했습니다. 다시 시도해주세요.")))
                .bodyToMono(KakaoUserResponseDto.class);
    }
}
