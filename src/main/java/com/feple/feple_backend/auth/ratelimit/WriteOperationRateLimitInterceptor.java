package com.feple.feple_backend.auth.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class WriteOperationRateLimitInterceptor implements HandlerInterceptor {

    private final WriteOperationRateLimiter mutationRateLimiter;

    private static final java.util.Set<String> MUTATION_METHODS =
            java.util.Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (MUTATION_METHODS.contains(request.getMethod().toUpperCase())) {
            mutationRateLimiter.check(resolveKey(request));
        }
        return true;
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return "user:" + userId;
        }
        return "ip:" + request.getRemoteAddr();
    }
}
