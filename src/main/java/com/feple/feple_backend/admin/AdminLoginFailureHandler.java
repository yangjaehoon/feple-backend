package com.feple.feple_backend.admin;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 관리자 로그인 실패 횟수를 IP 기준으로 제한한다.
 * 10분 동안 최대 5회 실패 허용, 초과 시 429 응답.
 */
@Component
public class AdminLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public AdminLoginFailureHandler() {
        setDefaultFailureUrl("/admin/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, jakarta.servlet.ServletException {
        String ip = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofMinutes(10))
                        .build())
                .build());

        if (!bucket.tryConsume(1)) {
            response.sendError(429, "로그인 시도가 너무 많습니다. 10분 후 다시 시도해주세요.");
            return;
        }

        super.onAuthenticationFailure(request, response, exception);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) return request.getRemoteAddr();
        return ip.split(",")[0].trim();
    }
}
