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

    private static final java.util.Set<String> MUTATION_METHODS =
            java.util.Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (MUTATION_METHODS.contains(request.getMethod().toUpperCase())) {
            mutationRateLimiter.check(request.getRemoteAddr());
        }
        return true;
    }
}
