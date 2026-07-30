package com.feple.feple_backend.admin.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

// Spring Security의 authorizeHttpRequests/@PreAuthorize 대신 HandlerInterceptor를 사용하는 이유:
// 권한(AdminPermission)이 정적 역할이 아닌 AdminAccount 엔티티에 DB로 저장된 per-account 설정이므로
// 어노테이션 값 자체는 SimpleGrantedAuthority 문자열 비교로 런타임에 검사해야 한다. 다만 "이 컨트롤러에
// 어떤 권한이 필요한지"는 @RequiresAdminPermission/@RequiresSuperAdmin으로 컨트롤러 클래스에 직접
// 선언하고(AdminPermissionAnnotationValidator가 기동 시점에 누락을 검증), 이 인터셉터는 그 선언을 읽어
// SecurityContext의 authority 목록과 대조하기만 한다.
@Component
public class AdminPermissionInterceptor implements HandlerInterceptor {

    // 어떤 어노테이션도 요구하지 않고 모든 ADMIN에게 열려 있어야 하는 경로 — 대시보드 루트만 해당.
    // 앱 구조상 거의 변하지 않는 고정 경로라 별도 어노테이션 없이 URI로 직접 예외 처리한다.
    private static final Set<String> OPEN_TO_ANY_ADMIN = Set.of("/admin", "/admin/");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // SecurityConfig의 adminFilterChain이 /admin/** 전체에 hasRole("ADMIN")을 강제하므로
        // 이 시점엔 이미 인증된 ADMIN이어야 함. 그래도 방어적으로 미인증 요청은 거부한다(fail-closed).
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return denyAccess(response);
        }

        String uri = request.getRequestURI();
        if (OPEN_TO_ANY_ADMIN.contains(uri)) {
            return true;
        }

        // InterceptorConfig가 이 인터셉터를 /admin/**에만 등록하고 정적 리소스(/css, /js, /img)는
        // 대상에서 제외하므로, 이 시점의 handler는 항상 컨트롤러 메서드(HandlerMethod)여야 한다.
        if (!(handler instanceof HandlerMethod hm)) {
            return denyAccess(response);
        }

        Class<?> controllerClass = hm.getBeanType();
        String requiredAuthority;
        if (controllerClass.isAnnotationPresent(RequiresSuperAdmin.class)) {
            requiredAuthority = "ROLE_SUPER_ADMIN";
        } else if (controllerClass.isAnnotationPresent(RequiresAdminPermission.class)) {
            AdminPermission required = controllerClass.getAnnotation(RequiresAdminPermission.class).value();
            requiredAuthority = "PERM_" + required.name();
        } else {
            // 정상 배포라면 AdminPermissionAnnotationValidator가 기동 시점에 먼저 앱을 실패시켜야
            // 도달하지 않는 경로다. 그래도 방어적으로 차단한다(fail-closed).
            return denyAccess(response);
        }

        if (!auth.getAuthorities().contains(new SimpleGrantedAuthority(requiredAuthority))) {
            return denyAccess(response);
        }

        return true;
    }

    private static boolean denyAccess(HttpServletResponse response) throws IOException {
        response.sendRedirect("/admin/access-denied");
        return false;
    }
}
