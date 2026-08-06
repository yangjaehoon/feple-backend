package com.feple.feple_backend.admin.auth;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 관리자 로그아웃을 감사 로그에 기록한다.
 * ObjectProvider 사용 이유는 AdminLoginSuccessHandler와 동일 (JPA 빈 조기 초기화 문제 회피).
 */
@Component
public class AdminLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ObjectProvider<AdminLogService> adminLogServiceProvider;

    public AdminLogoutSuccessHandler(ObjectProvider<AdminLogService> adminLogServiceProvider) {
        this.adminLogServiceProvider = adminLogServiceProvider;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
                                Authentication authentication) throws IOException {
        if (authentication != null) {
            adminLogServiceProvider.getObject()
                    .log(AdminAction.LOGOUT, "ADMIN_ACCOUNT", null, authentication.getName());
        }
        response.sendRedirect(request.getContextPath() + "/admin/login?logout=true");
    }
}
