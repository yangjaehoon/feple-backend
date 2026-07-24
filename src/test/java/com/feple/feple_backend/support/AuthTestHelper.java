package com.feple.feple_backend.support;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

/**
 * MockMvc 단위 테스트에서 @AuthenticationPrincipal Long userId 파라미터를 처리하기 위한
 * 인증 설정 헬퍼. standaloneSetup에서는 Spring Security 필터 체인이 없으므로
 * SecurityContextHolder에 직접 설정한다.
 */
public final class AuthTestHelper {

    private AuthTestHelper() {}

    public static RequestPostProcessor userAuth(Long userId) {
        return request -> {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            request.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context);
            // Authentication authentication 파라미터 (비어노테이션) 해결을 위해
            // request.userPrincipal도 설정한다.
            request.setUserPrincipal(auth);
            return request;
        };
    }
}
