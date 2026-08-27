package com.feple.feple_backend.admin.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

class AdminPermissionInterceptorTest {

    private final AdminPermissionInterceptor interceptor = new AdminPermissionInterceptor();

    @RequiresAdminPermission(AdminPermission.USERS)
    static class UsersController {
        public void handle() {}
    }

    @RequiresSuperAdmin
    static class SuperAdminController {
        public void handle() {}
    }

    static class UnannotatedController {
        public void handle() {}
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String uri) {
        return request(uri, "GET");
    }

    private MockHttpServletRequest request(String uri, String method) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.setMethod(method);
        return request;
    }

    private HandlerMethod handlerMethod(Class<?> controllerClass) throws Exception {
        Method method = controllerClass.getMethod("handle");
        return new HandlerMethod(controllerClass.getDeclaredConstructor().newInstance(), method);
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
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS_READ"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/users"), response, handlerMethod(UsersController.class));

        assertThat(result).isTrue();
    }

    @Test
    void 필요한_권한이_없으면_접근_거부() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_POSTS_READ"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/users"), response, handlerMethod(UsersController.class));

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/access-denied");
    }

    @Test
    void POST_요청은_WRITE_권한이_없으면_거부() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS_READ"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(
                request("/admin/users", "POST"), response, handlerMethod(UsersController.class));

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/access-denied");
    }

    @Test
    void POST_요청은_WRITE_권한이_있으면_통과() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS_WRITE"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(
                request("/admin/users", "POST"), response, handlerMethod(UsersController.class));

        assertThat(result).isTrue();
    }

    @Test
    void SUPER_ADMIN_컨트롤러는_ROLE_SUPER_ADMIN_없으면_접근_거부() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS_READ"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/accounts"), response, handlerMethod(SuperAdminController.class));

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/access-denied");
    }

    @Test
    void SUPER_ADMIN_컨트롤러는_ROLE_SUPER_ADMIN_있으면_통과() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/accounts"), response, handlerMethod(SuperAdminController.class));

        assertThat(result).isTrue();
    }

    // 어노테이션이 없는 컨트롤러는 무조건 차단한다 — 정상 배포라면 AdminPermissionAnnotationValidator가
    // 기동 시점에 먼저 앱을 실패시켜야 하지만, 인터셉터 자체도 방어적으로 fail-closed여야 한다.
    @Test
    void 권한_어노테이션이_없는_컨트롤러는_접근_거부() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS_READ"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/unregistered-feature"), response, handlerMethod(UnannotatedController.class));

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/access-denied");
    }

    @Test
    void 대시보드_루트는_어노테이션_없어도_통과() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS_READ"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin"), response, handlerMethod(UnannotatedController.class));

        assertThat(result).isTrue();
    }

    @Test
    void HandlerMethod가_아니면_접근_거부() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("PERM_USERS_READ"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request("/admin/users"), response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/access-denied");
    }
}
