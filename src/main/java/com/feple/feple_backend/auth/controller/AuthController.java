package com.feple.feple_backend.auth.controller;

import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.dto.FirebaseLoginRequest;
import com.feple.feple_backend.auth.dto.LocalLoginRequest;
import com.feple.feple_backend.auth.dto.RefreshRequest;
import com.feple.feple_backend.auth.jwt.JwtConstants;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.auth.ratelimit.LoginRateLimiter;
import com.feple.feple_backend.auth.service.LocalAuthService;
import com.feple.feple_backend.auth.service.OAuthLoginService;
import com.feple.feple_backend.auth.service.RefreshTokenService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@Tag(name = "인증", description = "카카오·Firebase OAuth 로그인, 토큰 갱신·로그아웃")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final OAuthLoginService kakaoAuthService;
    private final OAuthLoginService firebaseAuthService;
    private final LocalAuthService localAuthService;
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
            @Valid @RequestBody FirebaseLoginRequest req,
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

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LocalLoginRequest req,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.check(getClientIp(httpRequest));
        User user = localAuthService.login(req);
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.save(user.getId(), refreshToken);
        return ResponseEntity.ok(new AuthResponseDto(userService.toUserDto(user), accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshRequest req,
                                                   HttpServletRequest httpRequest) {
        loginRateLimiter.check(getClientIp(httpRequest));
        if (!jwtProvider.isRefreshToken(req.getRefreshToken())) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        Long userId = refreshTokenService.validateAndConsume(req.getRefreshToken());

        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenService.save(userId, newRefreshToken);

        UserResponseDto userDto = userService.getUser(userId);
        return ResponseEntity.ok(new AuthResponseDto(userDto, newAccessToken, newRefreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody RefreshRequest req,
                                                      HttpServletRequest httpRequest) {
        loginRateLimiter.check(getClientIp(httpRequest));
        if (req != null && req.getRefreshToken() != null) {
            refreshTokenService.revoke(req.getRefreshToken());
        }
        return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
