package com.feple.feple_backend.admin.auth;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AdminLoginSuccessHandlerTest {

    private AdminLoginSuccessHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private AdminLogService adminLogService;

    @BeforeEach
    void setUp() {
        adminLogService = mock(AdminLogService.class);
        handler = new AdminLoginSuccessHandler(adminLogService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        given(request.getContextPath()).willReturn("");
    }

    @Test
    void 로그인_성공시_감사로그_기록후_admin으로_리다이렉트() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("testadmin", null);

        handler.onAuthenticationSuccess(request, response, auth);

        verify(adminLogService).log(eq(AdminAction.LOGIN_SUCCESS), eq("ADMIN_ACCOUNT"), isNull(), eq("testadmin"));
        verify(response).sendRedirect("/admin");
    }
}
