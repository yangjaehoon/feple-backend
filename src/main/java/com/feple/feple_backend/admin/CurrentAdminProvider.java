package com.feple.feple_backend.admin;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** SecurityContext에서 현재 요청의 관리자 계정명을 안전하게 꺼낸다. 미인증이거나 조회 중 예외가 나도 null을 반환한다. */
@Component
public class CurrentAdminProvider {

    public String usernameOrNull() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
