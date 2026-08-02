package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * 관리자 로그인 실패 횟수를 클라이언트 IP 기준으로 제한하고, 실패 시도를 감사 로그에 남긴다.
 * 리버스 프록시 없이 JVM이 직접 트래픽을 받으므로 remoteAddr이 곧 실제 클라이언트 IP —
 * X-Forwarded-For는 신뢰하지 않는다(조작 시 이 제한이 우회될 수 있음).
 * 10분 동안 최대 5회 실패 허용, 초과 시 429 응답.
 * AdminLogService는 JPA 리포지토리에 의존하므로, SecurityConfig가 이 빈을 생성자에서 즉시
 * 주입받을 때 entityManagerFactory보다 먼저 초기화되지 않도록 ObjectProvider로 해석을 요청 시점까지 미룬다.
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

    private final ObjectProvider<AdminLogService> adminLogServiceProvider;

    public AdminLoginFailureHandler(ObjectProvider<AdminLogService> adminLogServiceProvider) {
        this.adminLogServiceProvider = adminLogServiceProvider;
    }

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
        String attemptedUsername = request.getParameter("username");
        if (!bucket.tryConsume(1)) {
            session.setAttribute(SESSION_KEY, "locked");
            logFailure(attemptedUsername, "locked");
            response.sendRedirect(request.getContextPath() + "/admin/login");
            return;
        }
        String reason = (exception instanceof DisabledException) ? "disabled" : "invalid";
        session.setAttribute(SESSION_KEY, reason);
        logFailure(attemptedUsername, reason);
        response.sendRedirect(request.getContextPath() + "/admin/login");
    }

    private void logFailure(String attemptedUsername, String reason) {
        String detail = (attemptedUsername != null && !attemptedUsername.isBlank() ? attemptedUsername : "(미입력)")
                + " — " + reason;
        adminLogServiceProvider.getObject().log(AdminAction.LOGIN_FAILURE, "ADMIN_ACCOUNT", null, detail);
    }
}
