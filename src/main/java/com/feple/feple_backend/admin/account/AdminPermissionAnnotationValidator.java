package com.feple.feple_backend.admin.account;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

// 새 admin 컨트롤러가 @RequiresAdminPermission/@RequiresSuperAdmin 없이 배포되는 것을 막는다.
// 등록된 모든 핸들러를 앱 기동 직후 훑어서 /admin/** 아래인데 두 어노테이션 모두 없는 컨트롤러가
// 있으면 즉시 예외를 던져 앱 기동 자체를 실패시킨다 — 운영 배포 후 사용자가 겪는 접근 거부 버그가
// 아니라 로컬 실행/CI 단계에서 바로 드러나게 하기 위함.
@Component
public class AdminPermissionAnnotationValidator implements ApplicationRunner {

    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/admin", "/admin/", "/admin/login", "/admin/logout", "/admin/access-denied", "/admin/search");

    private final RequestMappingHandlerMapping handlerMapping;

    // Spring Boot Actuator가 @ControllerEndpoint 지원을 위해 같은 타입의
    // "controllerEndpointHandlerMapping" 빈을 추가로 등록하므로, 타입만으로는 모호(NoUniqueBeanDefinitionException)
    // 하다 — Spring MVC의 표준 빈 이름으로 명시해야 한다.
    public AdminPermissionAnnotationValidator(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<Class<?>> checked = new HashSet<>();
        List<String> violations = new ArrayList<>();

        for (var entry : handlerMapping.getHandlerMethods().entrySet()) {
            Class<?> beanType = entry.getValue().getBeanType();
            if (!checked.add(beanType)) {
                continue;
            }

            if (!isUnderAdminAndNotExempt(entry.getKey())) {
                continue;
            }

            boolean hasAnnotation = beanType.isAnnotationPresent(RequiresAdminPermission.class)
                    || beanType.isAnnotationPresent(RequiresSuperAdmin.class);
            if (!hasAnnotation) {
                violations.add(beanType.getName());
            }
        }

        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    "다음 admin 컨트롤러에 @RequiresAdminPermission 또는 @RequiresSuperAdmin이 없습니다: "
                            + violations);
        }
    }

    private boolean isUnderAdminAndNotExempt(RequestMappingInfo info) {
        Set<String> patterns = info.getPatternValues();
        boolean underAdmin = patterns.stream().anyMatch(p -> p.equals("/admin") || p.startsWith("/admin/"));
        if (!underAdmin) {
            return false;
        }
        return !patterns.stream().allMatch(EXEMPT_PATHS::contains);
    }
}
