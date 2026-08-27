package com.feple.feple_backend.admin.account;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 이 컨트롤러에 접근하려면 AdminAccount에 해당 AdminPermission이 부여돼 있어야 함을 선언한다.
// AdminPermissionInterceptor가 이 어노테이션을 읽어 PERM_<value>_<READ|WRITE> 권한을 검사한다
// (조회 요청은 READ, 변경 요청은 WRITE). AdminPermissionAnnotationValidator가 시작 시점에 모든
// admin 컨트롤러에 이 어노테이션(또는 @RequiresSuperAdmin)이 붙어 있는지 검증한다 — 둘 다 없으면
// 앱이 기동에 실패한다.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresAdminPermission {
    AdminPermission value();

    /**
     * true면 조회(GET/HEAD)를 포함한 모든 요청에 WRITE 권한을 요구한다.
     * 화면 조회와 데이터 반출의 신뢰 수준이 다른 엔드포인트(CSV 내보내기 등)에 사용한다 —
     * 읽기 전용 관리자가 전체 데이터를 파일로 반출하지 못하게 한다.
     */
    boolean writeOnly() default false;
}
