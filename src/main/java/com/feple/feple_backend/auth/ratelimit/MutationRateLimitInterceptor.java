package com.feple.feple_backend.auth.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class MutationRateLimitInterceptor implements HandlerInterceptor {

    private final MutationRateLimiter mutationRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("POST".equalsIgnoreCase(request.getMethod())) {
            mutationRateLimiter.check(request.getRemoteAddr());
        }
        return true;
    }
}
