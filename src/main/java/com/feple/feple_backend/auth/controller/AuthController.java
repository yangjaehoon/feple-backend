package com.feple.feple_backend.auth.controller;

import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.dto.RefreshRequest;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.auth.kakao.KakaoApiClient;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

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

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@RequestBody RefreshRequest req) {

        if (!jwtProvider.isRefreshToken(req.getRefreshToken())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        Long userId = jwtProvider.parseUserId(req.getRefreshToken());
        UserResponseDto userDto = userService.getUser(userId);

        String newAccessToken = jwtProvider.createAccessToken(userId);
        return ResponseEntity.ok(new AuthResponseDto(userDto, newAccessToken));
    }
}