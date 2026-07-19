package com.feple.feple_backend.admin.account;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPermissionInterceptorTest {

    private final AdminPermissionInterceptor interceptor = new AdminPermissionInterceptor();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    // SecurityConfig가 /admin/**에 hasRole("ADMIN")을 강제하므로 실제로는 도달하지 않아야 하는 경로지만,
    // 방어적으로 미인증 요청은 거부해야 한다 (fail-closed 회귀 방지)
    @Test
    void 미인증_요청은_접근_거부() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/users"), response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/access-denied");
    }

    @Test
    void 익명_인증_토큰이면_접근_거부() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/users"), response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/access-denied");
    }

    @Test
    void 필요한_권한이_있으면_통과() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/users"), response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void 필요한_권한이_없으면_접근_거부() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_POSTS"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/users"), response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/access-denied");
    }

    @Test
    void SUPER_ADMIN_경로는_ROLE_SUPER_ADMIN_없으면_접근_거부() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/accounts"), response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/access-denied");
    }

    @Test
    void SUPER_ADMIN_경로는_ROLE_SUPER_ADMIN_있으면_통과() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/accounts"), response, new Object());

        assertThat(result).isTrue();
    }

    // PREFIX_MAP에 등록을 잊은 신규 경로가 모든 ADMIN에게 뚫리는 걸 막기 위한 fail-closed 회귀 방지 테스트
    @Test
    void 매핑되지_않은_경로는_접근_거부() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/unregistered-feature"), response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/access-denied");
    }

    @Test
    void 대시보드_루트는_별도_권한_없이_통과() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin"), response, new Object());

        assertThat(result).isTrue();
    }
}
