package com.feple.feple_backend.admin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 관리자 로그인 실패 횟수를 클라이언트 IP 기준으로 제한한다.
 * (forward-headers-strategy=native 설정으로 리버스 프록시 뒤에서도 X-Forwarded-For 기반 실제 IP 사용)
 * 10분 동안 최대 5회 실패 허용, 초과 시 429 응답.
 */
@Component
public class AdminLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public static final String SESSION_KEY = "loginError";

    private static final int  MAX_FAILURES   = 5;
    private static final int  WINDOW_MINUTES = 10;
    private static final long CACHE_MAX_SIZE = 5_000;

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(WINDOW_MINUTES, TimeUnit.MINUTES)
            .maximumSize(CACHE_MAX_SIZE)
            .build();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, jakarta.servlet.ServletException {
        String ip = request.getRemoteAddr();
        Bucket bucket = cache.get(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(MAX_FAILURES)
                        .refillGreedy(MAX_FAILURES, Duration.ofMinutes(WINDOW_MINUTES))
                        .build())
                .build());

        HttpSession session = request.getSession();
        if (!bucket.tryConsume(1)) {
            session.setAttribute(SESSION_KEY, "locked");
            response.sendRedirect(request.getContextPath() + "/admin/login");
            return;
        }
        if (exception instanceof DisabledException) {
            session.setAttribute(SESSION_KEY, "disabled");
        } else {
            session.setAttribute(SESSION_KEY, "invalid");
        }
        response.sendRedirect(request.getContextPath() + "/admin/login");
    }
}
