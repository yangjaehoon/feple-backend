package com.feple.feple_backend.service;

import com.feple.feple_backend.dto.user.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;


@Service
@RequiredArgsConstructor
public class KakaoAuthService {
    private final WebClient webClient = WebClient.create();

    public KakaoUserResponse getKakaoUserInfo(String accessToken) {
        return webClient.post()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("property_keys",
                        "[\"kakao_account.profile\",\"kakao_account.email\"]"))
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .block();
    }
}