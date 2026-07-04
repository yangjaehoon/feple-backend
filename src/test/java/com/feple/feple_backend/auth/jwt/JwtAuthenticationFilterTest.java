package com.feple.feple_backend.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import static org.mockito.Mockito.never;
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
        filter = new JwtAuthenticationFilter(jwtProvider, userRepository, new ObjectMapper().registerModule(new JavaTimeModule()));
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
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 밴된_사용자면_403_응답하고_필터체인_중단() throws Exception {
        String token = jwtProvider.createAccessToken(1L);
        User banned = activeUser(1L);
        banned.ban(7, "위반", "admin");
        given(userRepository.findById(1L)).willReturn(Optional.of(banned));
        MockHttpServletRequest request = authorizedRequest(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void 존재하지_않는_사용자면_401_응답하고_필터체인_중단() throws Exception {
        String token = jwtProvider.createAccessToken(99L);
        given(userRepository.findById(99L)).willReturn(Optional.empty());
        MockHttpServletRequest request = authorizedRequest(token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void 변조된_토큰이면_401_응답하고_필터체인_중단() throws Exception {
        MockHttpServletRequest request = authorizedRequest("this.is.not-a-valid-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }

    // 리프레시 토큰을 액세스 토큰 자리에 넣으면 parseUserId()가 IllegalArgumentException을 던짐 —
    // 이 예외가 조용히 삼켜지고 익명으로 통과되지 않는지 검증 (fail-open 회귀 방지)
    @Test
    void 리프레시_토큰을_액세스_토큰_자리에_넣으면_401_응답하고_필터체인_중단() throws Exception {
        String refreshToken = jwtProvider.createRefreshToken(1L);
        MockHttpServletRequest request = authorizedRequest(refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void Authorization_헤더_없으면_인증없이_필터체인_진행() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private MockHttpServletRequest authorizedRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", JwtConstants.BEARER_PREFIX + token);
        return request;
    }
}
