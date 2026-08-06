package com.feple.feple_backend.admin.auth;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 관리자 로그인 성공을 감사 로그에 기록한다.
 * AdminLogService는 JPA 리포지토리에 의존하므로, SecurityConfig가 이 빈을 생성자에서 즉시
 * 주입받을 때 entityManagerFactory보다 먼저 초기화되지 않도록 ObjectProvider로 해석을 요청 시점까지 미룬다.
 */
@Component
public class AdminLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectProvider<AdminLogService> adminLogServiceProvider;

    public AdminLoginSuccessHandler(ObjectProvider<AdminLogService> adminLogServiceProvider) {
        this.adminLogServiceProvider = adminLogServiceProvider;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        adminLogServiceProvider.getObject()
                .log(AdminAction.LOGIN_SUCCESS, "ADMIN_ACCOUNT", null, authentication.getName());
        response.sendRedirect(request.getContextPath() + "/admin");
    }
}
