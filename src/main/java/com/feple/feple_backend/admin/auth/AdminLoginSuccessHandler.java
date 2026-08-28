package com.feple.feple_backend.admin.auth;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 관리자 로그인 성공을 감사 로그에 기록한다.
 *
 * <p>AdminLogService는 JPA 리포지토리에 의존한다. 이 핸들러는 SecurityConfig가 생성자에서 바로
 * 주입받는데, 그 시점은 SecurityFilterChain 처리 중이라 entityManagerFactory 등록보다 앞설 수 있다.
 * 생성자 파라미터에 {@code @Lazy}를 붙여 실제 해석을 첫 로그인(요청 처리 시점)으로 미룬다.
 */
@Component
public class AdminLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AdminLogService adminLogService;

    public AdminLoginSuccessHandler(@Lazy AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        adminLogService.log(AdminAction.LOGIN_SUCCESS, "ADMIN_ACCOUNT", null, authentication.getName());
        response.sendRedirect(request.getContextPath() + "/admin");
    }
}
