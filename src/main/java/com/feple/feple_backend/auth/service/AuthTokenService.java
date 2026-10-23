package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 앱 자체 JWT(액세스·리프레시) 발급·갱신·폐기. 로그인 수단과 무관한 공통 단계다. */
@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    /** 로그인 완료 — 새 토큰 쌍을 발급하고 리프레시 토큰을 저장한다(기존 세션은 끊긴다). */
    public AuthResponseDto issue(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenService.save(user.getId(), refreshToken);
        return new AuthResponseDto(userService.toUserDto(user), accessToken, refreshToken);
    }

    /** 리프레시 토큰을 교체하고 새 액세스 토큰과 함께 돌려준다. */
    public AuthResponseDto refresh(String refreshToken) {
        if (!jwtProvider.isRefreshToken(refreshToken)) {
            throw new InvalidRequestException(RefreshTokenService.INVALID_REFRESH_TOKEN_MESSAGE);
        }
        RefreshTokenService.RotationResult rotation =
                refreshTokenService.rotate(refreshToken, jwtProvider::createRefreshToken);
        String accessToken = jwtProvider.createAccessToken(rotation.userId());
        UserResponseDto user = userService.getUser(rotation.userId());
        return new AuthResponseDto(user, accessToken, rotation.newRefreshToken());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}
