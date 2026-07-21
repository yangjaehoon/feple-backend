package com.feple.feple_backend.auth.jwt;

import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock UserRepository userRepository;
    @Mock FilterChain filterChain;

    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(
                "test-secret-key-for-jwt-signing-at-least-32-bytes-long",
                1000 * 60 * 15,
                1000 * 60 * 60 * 24 * 14);
        jwtProvider = new JwtProvider(props);
        filter = new JwtAuthenticationFilter(jwtProvider, userRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private User activeUser(Long id) {
        return User.builder().id(id).oauthId("o" + id).nickname("user" + id).role(UserRole.USER).build();
    }

    @Test
    void 유효한_액세스_토큰이면_인증_설정_후_필터체인_진행() throws Exception {
        String token = jwtProvider.createAccessToken(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(activeUser(1L)));
        MockHttpServletRequest request = authorizedRequest(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.JWT_FAILURE_ATTRIBUTE)).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // 만료·변조·정지 등으로 인증에 실패해도 이 필터 자체는 요청을 막지 않는다 — permitAll() 공개
    // 엔드포인트가 로그인 사용자의 토큰 만료만으로 차단되면 안 되기 때문에, 필터체인은 항상 진행하고
    // SecurityContext를 비운 채 실패 사유만 request attribute에 남긴다. 실제 보호가 필요한 엔드포인트는
    // Spring Security의 authorizeHttpRequests + AuthenticationEntryPoint(SecurityConfig)가 이 attribute를
    // 읽어 최종적으로 차단한다 — 그 경로는 SecurityConfig 통합 테스트 영역이라 여기서는 필터 자체의
    // 계약(= SecurityContext는 절대 잘못 설정되지 않는다)만 검증한다.

    @Test
    void 밴된_사용자면_실패사유_기록하고_인증은_설정안됨() throws Exception {
        String token = jwtProvider.createAccessToken(1L);
        User banned = activeUser(1L);
        banned.ban(7, "위반", "admin");
        given(userRepository.findById(1L)).willReturn(Optional.of(banned));
        MockHttpServletRequest request = authorizedRequest(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        JwtAuthenticationFilter.JwtFailure failure =
                (JwtAuthenticationFilter.JwtFailure) request.getAttribute(JwtAuthenticationFilter.JWT_FAILURE_ATTRIBUTE);
        assertThat(failure.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(failure.code()).isEqualTo(ErrorCode.USER_BANNED);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 존재하지_않는_사용자면_실패사유_기록하고_인증은_설정안됨() throws Exception {
        String token = jwtProvider.createAccessToken(99L);
        given(userRepository.findById(99L)).willReturn(Optional.empty());
        MockHttpServletRequest request = authorizedRequest(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        JwtAuthenticationFilter.JwtFailure failure =
                (JwtAuthenticationFilter.JwtFailure) request.getAttribute(JwtAuthenticationFilter.JWT_FAILURE_ATTRIBUTE);
        assertThat(failure.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(failure.code()).isEqualTo(ErrorCode.TOKEN_INVALID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 변조된_토큰이면_실패사유_기록하고_인증은_설정안됨() throws Exception {
        MockHttpServletRequest request = authorizedRequest("this.is.not-a-valid-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        JwtAuthenticationFilter.JwtFailure failure =
                (JwtAuthenticationFilter.JwtFailure) request.getAttribute(JwtAuthenticationFilter.JWT_FAILURE_ATTRIBUTE);
        assertThat(failure.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(failure.code()).isEqualTo(ErrorCode.TOKEN_INVALID);
        verify(filterChain).doFilter(request, response);
    }

    // 리프레시 토큰을 액세스 토큰 자리에 넣으면 parseUserId()가 예외를 던짐 — 이 경우에도
    // SecurityContext에 인증이 잘못 설정되지 않는지 검증 (fail-open 회귀 방지의 핵심 불변식)
    @Test
    void 리프레시_토큰을_액세스_토큰_자리에_넣으면_인증은_설정안됨() throws Exception {
        String refreshToken = jwtProvider.createRefreshToken(1L);
        MockHttpServletRequest request = authorizedRequest(refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        JwtAuthenticationFilter.JwtFailure failure =
                (JwtAuthenticationFilter.JwtFailure) request.getAttribute(JwtAuthenticationFilter.JWT_FAILURE_ATTRIBUTE);
        assertThat(failure.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(failure.code()).isEqualTo(ErrorCode.TOKEN_INVALID);
    }

    @Test
    void Authorization_헤더_없으면_인증없이_필터체인_진행() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(JwtAuthenticationFilter.JWT_FAILURE_ATTRIBUTE)).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest authorizedRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", JwtConstants.BEARER_PREFIX + token);
        return request;
    }
}
