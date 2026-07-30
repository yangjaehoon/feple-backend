package com.feple.feple_backend.admin.account;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 이 컨트롤러에 접근하려면 AdminAccount에 해당 AdminPermission이 부여돼 있어야 함을 선언한다.
// AdminPermissionInterceptor가 이 어노테이션을 읽어 PERM_<value> 권한을 검사하고,
// AdminPermissionAnnotationValidator가 시작 시점에 모든 admin 컨트롤러에 이 어노테이션(또는
// @RequiresSuperAdmin)이 붙어 있는지 검증한다 — 둘 다 없으면 앱이 기동에 실패한다.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAdminPermission {
    AdminPermission value();
}
