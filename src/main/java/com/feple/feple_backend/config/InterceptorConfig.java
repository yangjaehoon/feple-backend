package com.feple.feple_backend.config;

import com.feple.feple_backend.admin.account.AdminPermissionInterceptor;
import com.feple.feple_backend.auth.ratelimit.AdminWriteOperationRateLimitInterceptor;
import com.feple.feple_backend.auth.ratelimit.WriteOperationRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {

    private final AdminPermissionInterceptor adminPermissionInterceptor;
    private final WriteOperationRateLimitInterceptor mutationRateLimitInterceptor;
    private final AdminWriteOperationRateLimitInterceptor adminMutationRateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminPermissionInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login", "/admin/logout", "/admin/access-denied");

        registry.addInterceptor(adminMutationRateLimitInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login", "/admin/logout", "/admin/access-denied");

        // /admin/**은 별도 AdminWriteOperationRateLimitInterceptor가 담당, /auth/**는 컨트롤러에서
        // LoginRateLimiter를 직접 적용 — 그 외 인증 필요 API의 변경 요청(POST/PUT/PATCH/DELETE)은
        // 전부 이 인터셉터로 커버해 신규 엔드포인트 누락을 방지한다.
        //
        // 이 목록은 자동이 아니라 수동 allowlist다. 쓰기 엔드포인트가 있는 컨트롤러를 새로 만들면
        // 여기에 추가할 것 — /diaries/**가 빠져 있어 presign(10분짜리 S3 업로드 URL) 발급과
        // 일기 생성·수정·삭제가 분당 30회 제한을 전혀 받지 않았다.
        // 현재 제외된 경로는 /auth/**(위 참조)와 조회 전용인 /notices/**, /app/** 뿐이다.
        registry.addInterceptor(mutationRateLimitInterceptor)
                .addPathPatterns("/posts/**", "/comments/**", "/artists/**", "/artist-suggestions/**",
                        "/certifications/**", "/diaries/**", "/festivals/**", "/festival-suggestions/**",
                        "/notifications/**", "/users/**", "/search/**");
    }
}
