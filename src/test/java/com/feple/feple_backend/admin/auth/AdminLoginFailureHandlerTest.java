package com.feple.feple_backend.admin.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

class AdminLoginFailureHandlerTest {

    private AdminLoginFailureHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private AdminLogService adminLogService;

    @BeforeEach
    void setUp() {
        adminLogService = mock(AdminLogService.class);
        handler = new AdminLoginFailureHandler(adminLogService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        given(request.getRemoteAddr()).willReturn("1.2.3.4");
        given(request.getSession()).willReturn(session);
        given(request.getContextPath()).willReturn("");
        given(request.getParameter("username")).willReturn("testadmin");
    }

    @Test
    void 일반_인증실패시_invalid_세션_설정후_리다이렉트() throws Exception {
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("잘못된 인증"));

        verify(session).setAttribute(AdminLoginFailureHandler.SESSION_KEY, "invalid");
        verify(response).sendRedirect("/admin/login");
        verify(adminLogService).log(eq(AdminAction.LOGIN_FAILURE), eq("ADMIN_ACCOUNT"), isNull(), any());
    }

    @Test
    void 비활성_계정이어도_계정_열거_방지를_위해_invalid_세션_설정() throws Exception {
        handler.onAuthenticationFailure(request, response, new DisabledException("비활성 계정"));

        // 사용자에게는 항상 동일한 메시지를 보여준다 — "비활성화됨"을 그대로 노출하면
        // 아이디의 존재 여부(계정 상태)가 드러나는 계정 열거 공격에 악용될 수 있다.
        verify(session).setAttribute(AdminLoginFailureHandler.SESSION_KEY, "invalid");
        verify(response).sendRedirect("/admin/login");
        // 감사 로그에는 실제 사유("disabled")가 남아 관리자가 조사할 수 있어야 한다.
        verify(adminLogService).log(eq(AdminAction.LOGIN_FAILURE), eq("ADMIN_ACCOUNT"), isNull(),
                org.mockito.ArgumentMatchers.contains("disabled"));
    }

    @Test
    void 실패_5회_초과시_locked_세션_설정() throws Exception {
        for (int i = 0; i < 5; i++) {
            handler.onAuthenticationFailure(request, response, new BadCredentialsException("잘못된 인증"));
        }

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("잘못된 인증"));

        verify(session).setAttribute(AdminLoginFailureHandler.SESSION_KEY, "locked");
    }
}
