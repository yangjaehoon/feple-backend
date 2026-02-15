package com.feple.feple_backend.auth.controller;

import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.auth.kakao.KakaoApiClient;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final KakaoApiClient kakaoApiClient;
    private final UserService userService;
    private final JwtProvider jwtProvider;

    @PostMapping("/kakao")
    public Mono<AuthResponseDto> kakaoLogin(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        String kakaoAccessToken = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;

        return kakaoApiClient.getMe(kakaoAccessToken)
                .map(userService::registerOrLogin)
                .map(user -> new AuthResponseDto(
                        UserResponseDto.from(user),
                        jwtProvider.createAccessToken(user.getId())
                ));
    }
}