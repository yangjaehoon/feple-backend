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

    // GET이지만 SearchService.search()가 매 호출마다 SearchLog를 저장(write)하므로 함께 제한한다.
    // /search/suggestions는 저장을 하지 않아 제외 — 정확히 이 경로일 때만 적용.
    private static final String SEARCH_WRITE_PATH = "/search";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (MUTATION_METHODS.contains(request.getMethod().toUpperCase()) || isSearchWrite(request)) {
            mutationRateLimiter.check(resolveKey(request));
        }
        return true;
    }

    private boolean isSearchWrite(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod()) && SEARCH_WRITE_PATH.equals(request.getRequestURI());
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return "user:" + userId;
        }
        return "ip:" + request.getRemoteAddr();
    }
}
