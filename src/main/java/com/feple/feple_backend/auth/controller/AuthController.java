package com.feple.feple_backend.auth.controller;

import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.dto.ForgotPasswordRequest;
import com.feple.feple_backend.auth.dto.LocalLoginRequest;
import com.feple.feple_backend.auth.dto.RefreshRequest;
import com.feple.feple_backend.auth.dto.RegisterRequest;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.auth.kakao.KakaoApiClient;
import com.feple.feple_backend.auth.ratelimit.LoginRateLimiter;
import com.feple.feple_backend.auth.service.PasswordResetService;
import com.feple.feple_backend.auth.service.RefreshTokenService;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;
    private final PasswordResetService passwordResetService;

    @PostMapping("/kakao")
    public Mono<AuthResponseDto> kakaoLogin(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        String kakaoAccessToken = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;

        return kakaoApiClient.getMe(kakaoAccessToken)
                .map(userService::registerOrLogin)
                .map(user -> {
                    String accessToken = jwtProvider.createAccessToken(user.getId());
                    String refreshToken = jwtProvider.createRefreshToken(user.getId());
                    refreshTokenService.save(user.getId(), refreshToken);
                    return new AuthResponseDto(userService.toUserDto(user), accessToken, refreshToken);
                });
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequest req) {
        User user = userService.registerLocal(req);
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.save(user.getId(), refreshToken);
        return ResponseEntity.ok(new AuthResponseDto(userService.toUserDto(user), accessToken, refreshToken));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LocalLoginRequest req,
            HttpServletRequest httpRequest
    ) {
        String ip = getClientIp(httpRequest);
        loginRateLimiter.check(ip);

        User user = userService.loginLocal(req);
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.save(user.getId(), refreshToken);
        return ResponseEntity.ok(new AuthResponseDto(userService.toUserDto(user), accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@RequestBody RefreshRequest req) {
        if (!jwtProvider.isRefreshToken(req.getRefreshToken())) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // DB 검증 + 기존 토큰 삭제 (로테이션) → userId 반환
        Long userId = refreshTokenService.validateAndConsume(req.getRefreshToken());

        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenService.save(userId, newRefreshToken);

        UserResponseDto userDto = userService.getUser(userId);
        return ResponseEntity.ok(new AuthResponseDto(userDto, newAccessToken, newRefreshToken));
    }

    /**
     * 비밀번호 재설정 이메일 발송.
     * 가입된 이메일이 없어도 동일한 응답 반환 (이메일 열거 공격 방지).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        passwordResetService.requestReset(req.getEmail());
        return ResponseEntity.ok(Map.of("message", "가입된 이메일로 비밀번호 재설정 링크를 발송했습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody RefreshRequest req) {
        if (req.getRefreshToken() != null) {
            refreshTokenService.revoke(req.getRefreshToken());
        }
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim(); // 프록시 체인에서 첫 번째 IP
        }
        return ip;
    }
}
