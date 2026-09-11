package com.feple.feple_backend.admin.auth;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.auth.ratelimit.RateLimiterSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * 관리자 로그인 실패 횟수를 클라이언트 IP와 시도한 아이디(username) 양쪽 기준으로 제한하고,
 * 실패 시도를 감사 로그에 남긴다. IP 기준만 두면 공격자가 여러 IP(프록시 등)로 분산해
 * 특정 계정을 무제한으로 시도할 수 있으므로, 계정 단위 제한을 병행한다.
 * 아이디를 입력하지 않은 요청은 공유 키로 묶이는 것을 막기 위해 아이디 기준 제한에서 제외한다
 * (묶으면 서로 무관한 클라이언트들이 아이디 미입력만으로 하나의 버킷을 공유하게 됨).
 * 리버스 프록시 없이 JVM이 직접 트래픽을 받으므로 remoteAddr이 곧 실제 클라이언트 IP —
 * X-Forwarded-For는 신뢰하지 않는다(조작 시 이 제한이 우회될 수 있음).
 * 각 기준마다 10분 동안 최대 5회 실패 허용, 초과 시 429 응답.
 * AdminLogService는 JPA 리포지토리에 의존하고 이 핸들러는 SecurityFilterChain 처리 중에
 * 생성되므로, 생성자 파라미터의 {@code @Lazy}로 해석을 첫 요청 시점까지 미룬다.
 */
@Component
public class AdminLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    public static final String SESSION_KEY = "loginError";

    private static final String USERNAME_PARAM = "username";

    private static final int  MAX_FAILURES   = 5;
    private static final int  WINDOW_MINUTES = 10;
    private static final long CACHE_MAX_SIZE = 5_000;

    private final RateLimiterSupport ipLimiter = newLimiter();
    private final RateLimiterSupport usernameLimiter = newLimiter();

    private final AdminLogService adminLogService;

    public AdminLoginFailureHandler(@Lazy AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, jakarta.servlet.ServletException {
        HttpSession session = request.getSession();
        String attemptedUsername = request.getParameter(USERNAME_PARAM);

        String lockReason = rateLimitLockReason(request.getRemoteAddr(), attemptedUsername);
        if (lockReason != null) {
            lockOut(session, response, request, attemptedUsername, lockReason);
            return;
        }

        String reason = (exception instanceof DisabledException) ? "disabled" : "invalid";
        // 로그인 페이지에는 계정 상태를 구분해 보여주지 않는다 — "비활성화된 계정" 메시지가
        // 유효한 아이디의 존재 여부를 노출시킬 수 있으므로(계정 열거 공격) 사용자에게는 항상
        // 동일한 메시지를 보여주고, 실제 사유는 감사 로그에만 남긴다.
        session.setAttribute(SESSION_KEY, "invalid");
        logFailure(attemptedUsername, reason);
        response.sendRedirect(request.getContextPath() + "/admin/login");
    }

    /** 두 제한 중 하나라도 초과했으면 어느 쪽인지 담은 사유 문자열을, 아니면 {@code null}을 반환한다. */
    private String rateLimitLockReason(String ip, String attemptedUsername) {
        boolean ipAllowed = ipLimiter.tryConsume(ip);
        String usernameKey = normalizeUsername(attemptedUsername);
        boolean usernameAllowed = usernameKey.isEmpty() || usernameLimiter.tryConsume(usernameKey);

        if (ipAllowed && usernameAllowed) {
            return null;
        }
        if (!ipAllowed && !usernameAllowed) {
            return "locked(ip+username)";
        }
        return ipAllowed ? "locked(username)" : "locked(ip)";
    }

    private void lockOut(HttpSession session, HttpServletResponse response, HttpServletRequest request,
                          String attemptedUsername, String reason) throws IOException {
        session.setAttribute(SESSION_KEY, "locked");
        logFailure(attemptedUsername, reason);
        response.sendRedirect(request.getContextPath() + "/admin/login");
    }

    private void logFailure(String attemptedUsername, String reason) {
        String detail = (attemptedUsername != null && !attemptedUsername.isBlank() ? attemptedUsername : "(미입력)")
                + " — " + reason;
        adminLogService.log(AdminAction.LOGIN_FAILURE, "ADMIN_ACCOUNT", null, detail);
    }

    private static RateLimiterSupport newLimiter() {
        return new RateLimiterSupport(Duration.ofMinutes(WINDOW_MINUTES), CACHE_MAX_SIZE,
                MAX_FAILURES, Duration.ofMinutes(WINDOW_MINUTES));
    }

    private static String normalizeUsername(String username) {
        return (username == null || username.isBlank()) ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
