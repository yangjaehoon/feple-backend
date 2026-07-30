package com.feple.feple_backend.admin.account;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 이 컨트롤러는 ROLE_SUPER_ADMIN 권한을 가진 관리자만 접근할 수 있음을 선언한다.
// AdminPermissionInterceptor / AdminPermissionAnnotationValidator에서
// @RequiresAdminPermission과 동일한 방식으로 처리된다.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresSuperAdmin {
}
