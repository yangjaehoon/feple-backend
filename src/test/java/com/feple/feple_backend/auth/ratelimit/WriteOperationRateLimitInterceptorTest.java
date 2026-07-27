package com.feple.feple_backend.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class WriteOperationRateLimitInterceptorTest {

    @Mock WriteOperationRateLimiter mutationRateLimiter;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    private final WriteOperationRateLimitInterceptor interceptor =
            new WriteOperationRateLimitInterceptor(mutationRateLimiter);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void GET_요청은_제한_검사_안함() {
        given(request.getMethod()).willReturn("GET");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(mutationRateLimiter, never()).check(any());
    }

    @Test
    void POST_요청은_인증된_사용자ID로_검사() {
        given(request.getMethod()).willReturn("POST");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(10L, null));

        interceptor.preHandle(request, response, new Object());

        verify(mutationRateLimiter).check("user:10");
    }

    @Test
    void POST_요청_미인증이면_IP로_검사() {
        given(request.getMethod()).willReturn("POST");
        given(request.getRemoteAddr()).willReturn("1.2.3.4");

        interceptor.preHandle(request, response, new Object());

        verify(mutationRateLimiter).check("ip:1.2.3.4");
    }

    @Test
    void PUT_PATCH_DELETE도_변경요청으로_검사() {
        for (String method : new String[]{"PUT", "PATCH", "DELETE"}) {
            SecurityContextHolder.clearContext();
            given(request.getMethod()).willReturn(method);
            given(request.getRemoteAddr()).willReturn("1.2.3.4");

            interceptor.preHandle(request, response, new Object());
        }

        verify(mutationRateLimiter, times(3)).check("ip:1.2.3.4");
    }
}
