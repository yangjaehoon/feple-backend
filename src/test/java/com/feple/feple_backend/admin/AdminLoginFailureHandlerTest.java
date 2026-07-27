package com.feple.feple_backend.admin;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    @BeforeEach
    void setUp() {
        handler = new AdminLoginFailureHandler();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        given(request.getRemoteAddr()).willReturn("1.2.3.4");
        given(request.getSession()).willReturn(session);
        given(request.getContextPath()).willReturn("");
    }

    @Test
    void 일반_인증실패시_invalid_세션_설정후_리다이렉트() throws Exception {
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("잘못된 인증"));

        verify(session).setAttribute(AdminLoginFailureHandler.SESSION_KEY, "invalid");
        verify(response).sendRedirect("/admin/login");
    }

    @Test
    void 비활성_계정이면_disabled_세션_설정() throws Exception {
        handler.onAuthenticationFailure(request, response, new DisabledException("비활성 계정"));

        verify(session).setAttribute(AdminLoginFailureHandler.SESSION_KEY, "disabled");
        verify(response).sendRedirect("/admin/login");
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
