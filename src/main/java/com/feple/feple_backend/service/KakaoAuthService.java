package com.feple.feple_backend.service;

import com.feple.feple_backend.dto.user.KakaoUserResponse;
import io.netty.handler.codec.http2.Http2Headers;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://kapi.kakao.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8")
            .build();

    public KakaoUserResponse getKakaoUserInfo(String accessToken) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/user/me")
                        .queryParam("secure_resource", "true")  // 필요 시만 추가
                        .queryParam("property_keys", "[\"kakao_account.email\"]") // JSON 배열 문자열
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .block();
    }
}