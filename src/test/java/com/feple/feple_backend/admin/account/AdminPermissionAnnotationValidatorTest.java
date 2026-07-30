package com.feple.feple_backend.admin.account;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@ExtendWith(MockitoExtension.class)
class AdminPermissionAnnotationValidatorTest {

    @Mock
    RequestMappingHandlerMapping handlerMapping;

    @RequiresAdminPermission(AdminPermission.USERS)
    static class AnnotatedController {
        public void handle() {}
    }

    static class UnannotatedAdminController {
        public void handle() {}
    }

    static class NonAdminController {
        public void handle() {}
    }

    static class DashboardController {
        public void handle() {}
    }

    static class LoginController {
        public void handle() {}
    }

    private HandlerMethod handlerMethod(Class<?> controllerClass) throws Exception {
        Method method = controllerClass.getMethod("handle");
        return new HandlerMethod(controllerClass.getDeclaredConstructor().newInstance(), method);
    }

    private RequestMappingInfo mappingFor(String path) {
        return RequestMappingInfo.paths(path).build();
    }

    @Test
    void 모든_admin_컨트롤러에_어노테이션이_있으면_예외_없음() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(mappingFor("/admin/users"), handlerMethod(AnnotatedController.class));
        given(handlerMapping.getHandlerMethods()).willReturn(methods);

        AdminPermissionAnnotationValidator validator = new AdminPermissionAnnotationValidator(handlerMapping);

        validator.run(null);
    }

    @Test
    void 어노테이션_없는_admin_컨트롤러가_있으면_예외() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(mappingFor("/admin/unregistered-feature"), handlerMethod(UnannotatedAdminController.class));
        given(handlerMapping.getHandlerMethods()).willReturn(methods);

        AdminPermissionAnnotationValidator validator = new AdminPermissionAnnotationValidator(handlerMapping);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(UnannotatedAdminController.class.getName());
    }

    @Test
    void admin_경로가_아니면_무시() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(mappingFor("/festivals"), handlerMethod(NonAdminController.class));
        given(handlerMapping.getHandlerMethods()).willReturn(methods);

        AdminPermissionAnnotationValidator validator = new AdminPermissionAnnotationValidator(handlerMapping);

        validator.run(null);
    }

    @Test
    void 대시보드_루트와_로그인_경로는_어노테이션_없어도_통과() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(mappingFor("/admin"), handlerMethod(DashboardController.class));
        methods.put(mappingFor("/admin/login"), handlerMethod(LoginController.class));
        given(handlerMapping.getHandlerMethods()).willReturn(methods);

        AdminPermissionAnnotationValidator validator = new AdminPermissionAnnotationValidator(handlerMapping);

        validator.run(null);
    }

    @Test
    void 같은_컨트롤러_클래스는_한번만_검사() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> methods = new LinkedHashMap<>();
        methods.put(mappingFor("/admin/users"), handlerMethod(AnnotatedController.class));
        methods.put(mappingFor("/admin/users/1"), handlerMethod(AnnotatedController.class));
        given(handlerMapping.getHandlerMethods()).willReturn(methods);

        AdminPermissionAnnotationValidator validator = new AdminPermissionAnnotationValidator(handlerMapping);

        validator.run(null);
    }
}
