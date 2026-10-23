package com.feple.feple_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock JwtProvider jwtProvider;
    @Mock RefreshTokenService refreshTokenService;
    @Mock UserService userService;

    @InjectMocks AuthTokenService authTokenService;

    @Test
    void 로그인시_토큰쌍을_발급하고_리프레시_토큰을_저장() {
        User user = User.builder().id(1L).build();
        UserResponseDto userDto = mock(UserResponseDto.class);
        given(jwtProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token");
        given(userService.toUserDto(user)).willReturn(userDto);

        AuthResponseDto result = authTokenService.issue(user);

        assertThat(result).isEqualTo(new AuthResponseDto(userDto, "access-token", "refresh-token"));
        verify(refreshTokenService).save(1L, "refresh-token");
    }

    @Test
    void 리프레시_토큰이_아니면_교체하지_않고_400() {
        given(jwtProvider.isRefreshToken("bad-token")).willReturn(false);

        assertThatThrownBy(() -> authTokenService.refresh("bad-token"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage(RefreshTokenService.INVALID_REFRESH_TOKEN_MESSAGE);
        verify(refreshTokenService, never()).rotate(anyString(), any());
    }

    @Test
    void 갱신시_토큰을_교체하고_새_액세스_토큰과_함께_반환() {
        UserResponseDto userDto = mock(UserResponseDto.class);
        given(jwtProvider.isRefreshToken("valid-token")).willReturn(true);
        given(refreshTokenService.rotate(eq("valid-token"), any()))
                .willReturn(new RefreshTokenService.RotationResult(1L, "new-refresh-token"));
        given(jwtProvider.createAccessToken(1L)).willReturn("new-access-token");
        given(userService.getUser(1L)).willReturn(userDto);

        AuthResponseDto result = authTokenService.refresh("valid-token");

        assertThat(result).isEqualTo(new AuthResponseDto(userDto, "new-access-token", "new-refresh-token"));
    }

    @Test
    void 로그아웃은_리프레시_토큰_폐기에_위임() {
        authTokenService.logout("some-token");

        verify(refreshTokenService).revoke("some-token");
    }
}
