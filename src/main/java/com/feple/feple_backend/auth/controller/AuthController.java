package com.feple.feple_backend.auth.controller;

import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.dto.FirebaseLoginRequestDto;
import com.feple.feple_backend.auth.dto.RefreshRequestDto;
import com.feple.feple_backend.auth.jwt.JwtConstants;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.auth.ratelimit.LoginRateLimiter;
import com.feple.feple_backend.auth.service.OAuthLoginService;
import com.feple.feple_backend.auth.service.RefreshTokenService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Tag(name = "인증", description = "카카오·Firebase OAuth 로그인, 토큰 갱신·로그아웃")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final OAuthLoginService kakaoAuthService;
    private final OAuthLoginService firebaseAuthService;
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/kakao")
    public Mono<ResponseEntity<AuthResponseDto>> kakaoLogin(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.check(getClientIp(httpRequest));
        String kakaoAccessToken = authorization.startsWith(JwtConstants.BEARER_PREFIX)
                ? authorization.substring(JwtConstants.BEARER_LENGTH)
                : authorization;
        return kakaoAuthService.authenticate(kakaoAccessToken)
                .map(user -> ResponseEntity.ok(issueTokens(user)));
    }

    @PostMapping("/firebase")
    public Mono<ResponseEntity<AuthResponseDto>> firebaseLogin(
            @Valid @RequestBody FirebaseLoginRequestDto req,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.check(getClientIp(httpRequest));
        return firebaseAuthService.authenticate(req.getIdToken())
                .map(user -> ResponseEntity.ok(issueTokens(user)));
    }

    private AuthResponseDto issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.save(user.getId(), refreshToken);
        return new AuthResponseDto(userService.toUserDto(user), accessToken, refreshToken);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshRequestDto req,
                                                   HttpServletRequest httpRequest) {
        loginRateLimiter.check(getClientIp(httpRequest));
        if (!jwtProvider.isRefreshToken(req.getRefreshToken())) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(
                req.getRefreshToken(), jwtProvider::createRefreshToken);

        String newAccessToken = jwtProvider.createAccessToken(rotation.userId());
        UserResponseDto userDto = userService.getUser(rotation.userId());
        return ResponseEntity.ok(new AuthResponseDto(userDto, newAccessToken, rotation.newRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequestDto req,
                                       HttpServletRequest httpRequest) {
        loginRateLimiter.check(getClientIp(httpRequest));
        if (req != null && req.getRefreshToken() != null) {
            refreshTokenService.revoke(req.getRefreshToken());
        }
        return ResponseEntity.noContent().build();
    }

    // 리버스 프록시 없이 JVM이 직접 트래픽을 받으므로 remoteAddr이 곧 실제 클라이언트 IP —
    // X-Forwarded-For는 신뢰하지 않는다(조작 시 로그인 시도 제한이 우회될 수 있음)
    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
