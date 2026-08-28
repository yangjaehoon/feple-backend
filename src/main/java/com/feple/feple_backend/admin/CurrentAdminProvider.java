package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.account.AdminRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** SecurityContext에서 현재 요청의 관리자 정보를 안전하게 꺼낸다. 미인증이거나 조회 중 예외가 나도 안전한 기본값을 반환한다. */
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

    public boolean isSuperAdmin() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> AdminRole.SUPER_ADMIN.authority().equals(a.getAuthority()));
        } catch (Exception ignored) {
            return false;
        }
    }
}
