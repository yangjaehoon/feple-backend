package com.feple.feple_backend.admin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(5_000)
            .build();

    public AdminLoginFailureHandler() {
        setDefaultFailureUrl("/admin/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, jakarta.servlet.ServletException {
        String ip = request.getRemoteAddr();
        Bucket bucket = cache.get(ip, k -> Bucket.builder()
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
}
