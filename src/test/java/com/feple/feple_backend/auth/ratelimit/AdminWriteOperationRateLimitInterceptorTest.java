package com.feple.feple_backend.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AdminWriteOperationRateLimitInterceptorTest {

    @Mock AdminWriteOperationRateLimiter adminMutationRateLimiter;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    @InjectMocks AdminWriteOperationRateLimitInterceptor interceptor;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void POST가_아니면_검사없이_통과() throws Exception {
        given(request.getMethod()).willReturn("GET");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(adminMutationRateLimiter, never()).tryConsume(any());
    }

    @Test
    void 인증된_관리자는_계정명으로_검사() throws Exception {
        given(request.getMethod()).willReturn("POST");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin1", "pw",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        given(adminMutationRateLimiter.tryConsume("admin1")).willReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(adminMutationRateLimiter).tryConsume("admin1");
    }

    @Test
    void 미인증이면_IP로_검사() throws Exception {
        given(request.getMethod()).willReturn("POST");
        given(request.getRemoteAddr()).willReturn("1.2.3.4");
        given(adminMutationRateLimiter.tryConsume("1.2.3.4")).willReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(adminMutationRateLimiter).tryConsume("1.2.3.4");
    }

    @Test
    void 한도초과시_429응답후_false반환() throws Exception {
        given(request.getMethod()).willReturn("POST");
        given(request.getRemoteAddr()).willReturn("1.2.3.4");
        given(adminMutationRateLimiter.tryConsume("1.2.3.4")).willReturn(false);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        verify(response).sendError(429, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
    }
}
