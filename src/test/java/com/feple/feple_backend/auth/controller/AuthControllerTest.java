package com.feple.feple_backend.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feple.feple_backend.auth.dto.AuthResponseDto;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.auth.ratelimit.LoginRateLimiter;
import com.feple.feple_backend.auth.ratelimit.TokenRefreshRateLimiter;
import com.feple.feple_backend.auth.service.AgeVerificationService;
import com.feple.feple_backend.auth.service.AuthTokenService;
import com.feple.feple_backend.auth.service.OAuthLoginService;
import com.feple.feple_backend.global.exception.GlobalExceptionHandler;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock OAuthLoginService kakaoAuthService;
    @Mock OAuthLoginService firebaseAuthService;
    @Mock AuthTokenService authTokenService;
    @Mock JwtProvider jwtProvider;
    @Mock LoginRateLimiter loginRateLimiter;
    @Mock TokenRefreshRateLimiter tokenRefreshRateLimiter;
    @Mock AgeVerificationService ageVerificationService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                kakaoAuthService, firebaseAuthService,
                authTokenService, jwtProvider, loginRateLimiter,
                tokenRefreshRateLimiter, ageVerificationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 리프레시_유효하지않은_토큰_400() throws Exception {
        given(authTokenService.refresh("bad-token"))
                .willThrow(new InvalidRequestException("유효하지 않은 리프레시 토큰입니다."));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"bad-token\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 리프레시_성공() throws Exception {
        given(authTokenService.refresh("valid-token")).willReturn(tokens());

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

        verify(authTokenService).logout("some-token");
    }

    @Test
    void 카카오_로그인_Bearer_접두어_제거후_인증() throws Exception {
        User user = mock(User.class);
        given(kakaoAuthService.authenticate("raw-kakao-token")).willReturn(Mono.just(user));
        given(authTokenService.issue(user)).willReturn(tokens());

        mockMvc.perform(post("/auth/kakao")
                        .header("Authorization", "Bearer raw-kakao-token"))
                .andExpect(status().isOk());

        verify(kakaoAuthService).authenticate("raw-kakao-token");
    }

    @Test
    void 카카오_로그인_Bearer_접두어_없으면_그대로_사용() throws Exception {
        User user = mock(User.class);
        given(kakaoAuthService.authenticate("raw-kakao-token")).willReturn(Mono.just(user));
        given(authTokenService.issue(user)).willReturn(tokens());

        mockMvc.perform(post("/auth/kakao")
                        .header("Authorization", "raw-kakao-token"))
                .andExpect(status().isOk());

        verify(kakaoAuthService).authenticate("raw-kakao-token");
    }

    @Test
    void 파이어베이스_로그인_성공() throws Exception {
        User user = mock(User.class);
        given(firebaseAuthService.authenticate(anyString())).willReturn(Mono.just(user));
        given(authTokenService.issue(user)).willReturn(tokens());

        mockMvc.perform(post("/auth/firebase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"firebase-id-token-xxx\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 카카오_토큰이_무효면_400() throws Exception {
        given(kakaoAuthService.authenticate("bad-token"))
                .willReturn(Mono.error(new InvalidRequestException("카카오 로그인에 실패했습니다. 다시 시도해주세요.")));

        var mvcResult = mockMvc.perform(post("/auth/kakao").header("Authorization", "Bearer bad-token"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isBadRequest());
    }

    @Test
    void 카카오_API가_5xx면_502() throws Exception {
        WebClientResponseException upstream = WebClientResponseException.create(
                HttpStatus.BAD_GATEWAY.value(), "Bad Gateway", null, null, null);
        given(kakaoAuthService.authenticate("token")).willReturn(Mono.error(upstream));

        var mvcResult = mockMvc.perform(post("/auth/kakao").header("Authorization", "Bearer token"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isBadGateway());
    }

    private static AuthResponseDto tokens() {
        return new AuthResponseDto(mock(UserResponseDto.class), "access-token", "refresh-token");
    }
}
