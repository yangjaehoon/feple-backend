package com.feple.feple_backend.auth.controller;

import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.dto.FirebaseLoginRequest;
import com.feple.feple_backend.auth.dto.LocalLoginRequest;
import com.feple.feple_backend.auth.dto.RefreshRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.auth.kakao.KakaoApiClient;
import com.feple.feple_backend.auth.ratelimit.LoginRateLimiter;
import com.feple.feple_backend.auth.service.AuthService;
import com.feple.feple_backend.auth.service.RefreshTokenService;
import com.feple.feple_backend.user.entity.User;
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
    private final AuthService authService;
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/kakao")
    public Mono<AuthResponseDto> kakaoLogin(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.check(getClientIp(httpRequest));
        String kakaoAccessToken = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;

        return kakaoApiClient.getMe(kakaoAccessToken)
                .map(authService::registerOrLogin)
                .map(user -> {
                    String accessToken = jwtProvider.createAccessToken(user.getId());
                    String refreshToken = jwtProvider.createRefreshToken(user.getId());
                    refreshTokenService.save(user.getId(), refreshToken);
                    return new AuthResponseDto(userService.toUserDto(user), accessToken, refreshToken);
                });
    }

    /** Firebase ID 토큰 검증 후 앱 JWT 발급 (이메일/비밀번호 Firebase 로그인) */
    @PostMapping("/firebase")
    public ResponseEntity<AuthResponseDto> firebaseLogin(
            @Valid @RequestBody FirebaseLoginRequest req,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.check(getClientIp(httpRequest));
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(req.getIdToken());

            // 이메일 인증 확인
            Boolean emailVerified = (Boolean) decoded.getClaims().get("email_verified");
            if (emailVerified == null || !emailVerified) {
                throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
            }

            String uid = decoded.getUid();
            String email = decoded.getEmail();
            String name = decoded.getName();

            User user = authService.registerOrLoginFirebase(uid, email, name);
            String accessToken = jwtProvider.createAccessToken(user.getId());
            String refreshToken = jwtProvider.createRefreshToken(user.getId());
            refreshTokenService.save(user.getId(), refreshToken);
            return ResponseEntity.ok(new AuthResponseDto(userService.toUserDto(user), accessToken, refreshToken));
        } catch (Exception e) {
            throw new IllegalArgumentException("인증에 실패했습니다. 다시 로그인해주세요.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LocalLoginRequest req,
            HttpServletRequest httpRequest
    ) {
        String ip = getClientIp(httpRequest);
        loginRateLimiter.check(ip);

        User user = authService.loginLocal(req);
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.save(user.getId(), refreshToken);
        return ResponseEntity.ok(new AuthResponseDto(userService.toUserDto(user), accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@RequestBody RefreshRequest req,
                                                   HttpServletRequest httpRequest) {
        loginRateLimiter.check(getClientIp(httpRequest));
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

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody RefreshRequest req) {
        if (req.getRefreshToken() != null) {
            refreshTokenService.revoke(req.getRefreshToken());
        }
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    private String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For는 클라이언트가 위조 가능하므로 실제 TCP 연결 IP를 사용
        return request.getRemoteAddr();
    }
}
