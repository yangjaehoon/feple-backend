package com.feple.feple_backend.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.global.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    void GET이면_검사없이_통과() throws Exception {
        given(request.getMethod()).willReturn("GET");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(adminMutationRateLimiter, never()).tryConsume(any());
    }

    @Test
    void 메서드가_null이면_예외없이_통과() throws Exception {
        given(request.getMethod()).willReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(adminMutationRateLimiter, never()).tryConsume(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "PATCH", "DELETE"})
    void 상태변경_메서드는_POST와_동일하게_검사한다(String method) {
        given(request.getMethod()).willReturn(method);
        given(request.getRemoteAddr()).willReturn("1.2.3.4");
        given(adminMutationRateLimiter.tryConsume("1.2.3.4")).willReturn(false);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(TooManyRequestsException.class);
        verify(adminMutationRateLimiter).tryConsume("1.2.3.4");
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
    void 한도초과시_TooManyRequestsException_발생() throws Exception {
        // sendError(429)는 /error 포워딩으로 메시지가 사라지고 Whitelabel 페이지가 떴다.
        // 이제 다른 limiter와 동일하게 예외를 던져 AdminExceptionAdvice가 렌더한다.
        given(request.getMethod()).willReturn("POST");
        given(request.getRemoteAddr()).willReturn("1.2.3.4");
        given(adminMutationRateLimiter.tryConsume("1.2.3.4")).willReturn(false);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("요청이 너무 많습니다");
        verify(response, never()).sendError(anyInt(), any());
    }
}
