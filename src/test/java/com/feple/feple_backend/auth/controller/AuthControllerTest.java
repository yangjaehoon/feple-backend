package com.feple.feple_backend.auth.controller;

import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.dto.LocalLoginRequestDto;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.auth.ratelimit.LoginRateLimiter;
import com.feple.feple_backend.auth.service.LocalAuthService;
import com.feple.feple_backend.auth.service.OAuthLoginService;
import com.feple.feple_backend.auth.service.RefreshTokenService;
import com.feple.feple_backend.global.exception.GlobalExceptionHandler;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock OAuthLoginService kakaoAuthService;
    @Mock OAuthLoginService firebaseAuthService;
    @Mock LocalAuthService localAuthService;
    @Mock UserService userService;
    @Mock JwtProvider jwtProvider;
    @Mock RefreshTokenService refreshTokenService;
    @Mock LoginRateLimiter loginRateLimiter;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                kakaoAuthService, firebaseAuthService, localAuthService,
                userService, jwtProvider, refreshTokenService, loginRateLimiter);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 로컬_로그인_성공() throws Exception {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(localAuthService.login(any(LocalLoginRequestDto.class))).willReturn(user);
        given(jwtProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token");
        UserResponseDto userDto = mock(UserResponseDto.class);
        given(userService.toUserDto(user)).willReturn(userDto);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"pass123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 리프레시_유효하지않은_토큰_400() throws Exception {
        given(jwtProvider.isRefreshToken("bad-token")).willReturn(false);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bad-token\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 리프레시_성공() throws Exception {
        given(jwtProvider.isRefreshToken("valid-token")).willReturn(true);
        given(refreshTokenService.validateAndConsume("valid-token")).willReturn(1L);
        given(jwtProvider.createAccessToken(1L)).willReturn("new-access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("new-refresh-token");
        UserResponseDto userDto = mock(UserResponseDto.class);
        given(userService.getUser(1L)).willReturn(userDto);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"valid-token\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 로그아웃_성공() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"some-token\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 파이어베이스_로그인_성공() throws Exception {
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);
        given(firebaseAuthService.authenticate(anyString())).willReturn(Mono.just(user));
        given(jwtProvider.createAccessToken(1L)).willReturn("access-token");
        given(jwtProvider.createRefreshToken(1L)).willReturn("refresh-token");
        UserResponseDto userDto = mock(UserResponseDto.class);
        given(userService.toUserDto(user)).willReturn(userDto);

        mockMvc.perform(post("/auth/firebase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"firebase-id-token-xxx\"}"))
                .andExpect(status().isOk());
    }
}
