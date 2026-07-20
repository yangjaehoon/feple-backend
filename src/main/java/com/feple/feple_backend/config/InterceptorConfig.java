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
        // 전부 이 인터셉터로 커버해 신규 엔드포인트 추가 시 rate limit 누락을 방지한다.
        registry.addInterceptor(mutationRateLimitInterceptor)
                .addPathPatterns("/posts/**", "/comments/**", "/artists/**", "/artist-suggestions/**",
                        "/certifications/**", "/festivals/**", "/notifications/**", "/users/**", "/search/**");
    }
}
