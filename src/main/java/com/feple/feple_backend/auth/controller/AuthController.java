package com.feple.feple_backend.auth.controller;

import com.feple.feple_backend.auth.dto.AgeVerificationRequestDto;
import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.dto.FirebaseLoginRequestDto;
import com.feple.feple_backend.auth.dto.RefreshRequestDto;
import com.feple.feple_backend.auth.jwt.JwtConstants;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.auth.ratelimit.LoginRateLimiter;
import com.feple.feple_backend.auth.service.AgeVerificationService;
import com.feple.feple_backend.auth.service.OAuthLoginService;
import com.feple.feple_backend.auth.service.RefreshTokenService;
import com.feple.feple_backend.global.exception.AuthenticationRequiredException;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.service.UserService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    private final AgeVerificationService ageVerificationService;

    @PostMapping("/kakao")
    public Mono<ResponseEntity<AuthResponseDto>> kakaoLogin(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.check(getClientIp(httpRequest));
        return kakaoAuthService.authenticate(bearerToken(authorization))
                .map(user -> ResponseEntity.ok(issueTokens(user)));
    }

    /**
     * 나이 확인 게이트 — 첫 로그인 직후 생년월일을 제출한다. 만 14세 이상이면 204,
     * 미만이면 계정이 파기되고 403(AGE_RESTRICTED)이 반환된다.
     * JwtAuthenticationFilter는 {@code /auth/**}를 건너뛰므로, 발급받은 액세스 토큰을
     * Authorization 헤더로 직접 받아 사용자 ID를 해석한다.
     */
    @PostMapping("/age-verification")
    public ResponseEntity<Void> verifyAge(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody AgeVerificationRequestDto req
    ) {
        // 유효한 액세스 토큰이 있어야만 동작하고, 계정당 실질적으로 1회만 유효한 작업이라
        // (이후 재제출은 no-op) 로그인 버킷 rate limit은 적용하지 않는다 — 배포 직후 모든
        // 기존 유저가 이 단계를 거쳐야 하는데 CGNAT 공유 IP에서 429로 앱이 잠길 수 있다.
        Long userId = parseAccessTokenUserId(bearerToken(authorization));
        ageVerificationService.submitBirthDate(userId, req.birthDate());
        return ResponseEntity.noContent().build();
    }

    private String bearerToken(String authorization) {
        return authorization.startsWith(JwtConstants.BEARER_PREFIX)
                ? authorization.substring(JwtConstants.BEARER_LENGTH)
                : authorization;
    }

    private Long parseAccessTokenUserId(String accessToken) {
        try {
            return jwtProvider.parseUserId(accessToken);
        } catch (JwtException | IllegalArgumentException e) {
            // 만료·변조·빈 토큰 모두 401로 응답해, 클라이언트 인터셉터가 토큰을 갱신해
            // 재시도하도록 한다(나이 확인 화면에 오래 머물러 액세스 토큰이 만료된 경우 포함).
            throw new AuthenticationRequiredException("로그인이 필요합니다.");
        }
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
            throw new InvalidRequestException("유효하지 않은 리프레시 토큰입니다.");
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
