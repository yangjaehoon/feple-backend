package com.feple.feple_backend.auth.ratelimit;

import com.feple.feple_backend.global.exception.TooManyRequestsException;
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
        if (!HttpWriteMethods.isWriteMethod(request.getMethod())) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String key = (auth != null && auth.isAuthenticated()) ? auth.getName() : request.getRemoteAddr();
        // sendError는 /error로 포워딩되는데 server.error.include-message 기본값이 never라
        // 메시지가 사라지고 Whitelabel 페이지가 뜬다. 다른 limiter와 동일하게 예외를 던져
        // AdminExceptionAdvice가 관리자 에러 페이지(또는 JSON)로 렌더하게 한다.
        if (!adminMutationRateLimiter.tryConsume(key)) {
            throw new TooManyRequestsException(RateLimiterSupport.TOO_MANY_REQUESTS_MESSAGE);
        }
        return true;
    }
}
