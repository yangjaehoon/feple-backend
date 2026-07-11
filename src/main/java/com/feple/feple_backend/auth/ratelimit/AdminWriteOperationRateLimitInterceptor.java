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
public class AdminWriteOperationRateLimitInterceptor implements HandlerInterceptor {

    private final AdminWriteOperationRateLimiter adminMutationRateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String key = (auth != null && auth.isAuthenticated()) ? auth.getName() : request.getRemoteAddr();
        if (!adminMutationRateLimiter.tryConsume(key)) {
            response.sendError(429, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
            return false;
        }
        return true;
    }
}
