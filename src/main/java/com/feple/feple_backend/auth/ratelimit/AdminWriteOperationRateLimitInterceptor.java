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
            response.sendError(429, RateLimiterSupport.TOO_MANY_REQUESTS_MESSAGE);
            return false;
        }
        return true;
    }
}
