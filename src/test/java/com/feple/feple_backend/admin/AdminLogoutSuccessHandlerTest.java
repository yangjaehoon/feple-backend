package com.feple.feple_backend.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AdminLogoutSuccessHandlerTest {

    private AdminLogoutSuccessHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private AdminLogService adminLogService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        adminLogService = mock(AdminLogService.class);
        ObjectProvider<AdminLogService> adminLogServiceProvider = mock(ObjectProvider.class);
        given(adminLogServiceProvider.getObject()).willReturn(adminLogService);

        handler = new AdminLogoutSuccessHandler(adminLogServiceProvider);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        given(request.getContextPath()).willReturn("");
    }

    @Test
    void 로그아웃시_감사로그_기록후_로그인페이지로_리다이렉트() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("testadmin", null);

        handler.onLogoutSuccess(request, response, auth);

        verify(adminLogService).log(eq(AdminAction.LOGOUT), eq("ADMIN_ACCOUNT"), isNull(), eq("testadmin"));
        verify(response).sendRedirect("/admin/login?logout=true");
    }

    @Test
    void authentication이_null이면_감사로그_기록하지_않음() throws Exception {
        handler.onLogoutSuccess(request, response, null);

        verify(adminLogService, never()).log(any(), any(), any(), any());
        verify(response).sendRedirect("/admin/login?logout=true");
    }
}
