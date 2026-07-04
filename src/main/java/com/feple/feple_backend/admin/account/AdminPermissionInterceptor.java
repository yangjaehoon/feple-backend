package com.feple.feple_backend.admin.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AdminPermissionInterceptor implements HandlerInterceptor {

    // LinkedHashMap preserves insertion order for correct prefix matching
    // (longer/more-specific prefixes first where needed)
    private static final Map<String, AdminPermission> PREFIX_MAP = new LinkedHashMap<>();

    static {
        PREFIX_MAP.put("/admin/export/users",        AdminPermission.USERS);
        PREFIX_MAP.put("/admin/export/reports",      AdminPermission.REPORTS);
        PREFIX_MAP.put("/admin/stats",               AdminPermission.STATS);
        PREFIX_MAP.put("/admin/festivals",           AdminPermission.FESTIVALS);
        PREFIX_MAP.put("/admin/artist-suggestions",  AdminPermission.ARTISTS);
        PREFIX_MAP.put("/admin/setlist-requests",    AdminPermission.SONG_REQUESTS);
        PREFIX_MAP.put("/admin/artists",             AdminPermission.ARTISTS);
        PREFIX_MAP.put("/admin/posts",               AdminPermission.POSTS);
        PREFIX_MAP.put("/admin/users",               AdminPermission.USERS);
        PREFIX_MAP.put("/admin/certifications",      AdminPermission.CERTIFICATIONS);
        PREFIX_MAP.put("/admin/reports",             AdminPermission.REPORTS);
        PREFIX_MAP.put("/admin/song-requests",       AdminPermission.SONG_REQUESTS);
        PREFIX_MAP.put("/admin/bad-words",           AdminPermission.BAD_WORDS);
        PREFIX_MAP.put("/admin/crawl",               AdminPermission.CRAWL);
        PREFIX_MAP.put("/admin/logs",                AdminPermission.LOGS);
        PREFIX_MAP.put("/admin/accounts",            null); // SUPER_ADMIN only
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // SecurityConfig의 adminFilterChain이 /admin/** 전체에 hasRole("ADMIN")을 강제하므로
        // 이 시점엔 이미 인증된 ADMIN이어야 함. 그래도 방어적으로 미인증 요청은 거부한다(fail-closed).
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            response.sendRedirect("/admin/access-denied");
            return false;
        }

        String uri = request.getRequestURI();

        for (Map.Entry<String, AdminPermission> entry : PREFIX_MAP.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                AdminPermission required = entry.getValue();

                if (required == null) {
                    // SUPER_ADMIN only path
                    if (!auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))) {
                        response.sendRedirect("/admin/access-denied");
                        return false;
                    }
                } else {
                    String permAuthority = "PERM_" + required.name();
                    if (!auth.getAuthorities().contains(new SimpleGrantedAuthority(permAuthority))) {
                        response.sendRedirect("/admin/access-denied");
                        return false;
                    }
                }
                break;
            }
        }

        return true;
    }
}
