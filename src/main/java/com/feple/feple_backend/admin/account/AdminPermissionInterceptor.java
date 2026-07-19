package com.feple.feple_backend.admin.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// Spring Security의 authorizeHttpRequests/@PreAuthorize 대신 HandlerInterceptor를 사용하는 이유:
// 권한(AdminPermission)이 정적 역할이 아닌 AdminAccount 엔티티에 DB로 저장된 per-account 설정이므로
// 어노테이션으로 컴파일 타임에 선언할 수 없다. 런타임에 SecurityContext의 granted authority 목록을
// 이 인터셉터에서 검사한다.
@Component
public class AdminPermissionInterceptor implements HandlerInterceptor {

    // PREFIX_MAP의 Map 순서는 무관 — preHandle에서 가장 긴(가장 구체적인) prefix를 선택하므로
    // 예) /admin/artist-suggestions는 /admin/artists보다 길어 올바른 ARTISTS 권한으로 매칭됨.
    // value가 null인 경로는 SUPER_ADMIN 전용을 의미한다.
    private static final Map<String, AdminPermission> PREFIX_MAP = new HashMap<>();

    // 어떤 PREFIX_MAP 항목과도 매칭되지 않지만 별도 권한 없이 모든 ADMIN에게 열려 있어야 하는 경로.
    // 대시보드 루트만 해당 — 그 외 새 admin 경로를 추가할 때는 반드시 PREFIX_MAP에 등록해야 하며,
    // 등록을 잊으면 preHandle이 기본적으로 접근을 차단한다(fail-closed).
    private static final Set<String> OPEN_TO_ANY_ADMIN = Set.of("/admin", "/admin/");

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
        PREFIX_MAP.put("/admin/nickname-restrictions", AdminPermission.BAD_WORDS);
        PREFIX_MAP.put("/admin/crawl",               AdminPermission.CRAWL);
        PREFIX_MAP.put("/admin/logs",                AdminPermission.LOGS);
        PREFIX_MAP.put("/admin/accounts",            null); // SUPER_ADMIN 전용
        PREFIX_MAP.put("/admin/push",                null); // SUPER_ADMIN 전용
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // SecurityConfig의 adminFilterChain이 /admin/** 전체에 hasRole("ADMIN")을 강제하므로
        // 이 시점엔 이미 인증된 ADMIN이어야 함. 그래도 방어적으로 미인증 요청은 거부한다(fail-closed).
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return denyAccess(response);
        }

        String uri = request.getRequestURI();
        if (OPEN_TO_ANY_ADMIN.contains(uri)) {
            return true;
        }

        String matchedPrefix = null;
        for (String prefix : PREFIX_MAP.keySet()) {
            if (uri.startsWith(prefix) && (matchedPrefix == null || prefix.length() > matchedPrefix.length())) {
                matchedPrefix = prefix;
            }
        }

        // PREFIX_MAP에 등록되지 않은 경로는 신규 admin 기능 추가 시 등록을 잊은 것일 수 있으므로
        // 안전하게 차단한다(fail-closed). 모든 ADMIN에게 열어야 하는 경로는 OPEN_TO_ANY_ADMIN에 추가할 것.
        if (matchedPrefix == null) {
            return denyAccess(response);
        }

        AdminPermission required = PREFIX_MAP.get(matchedPrefix);
        String requiredAuthority = required == null ? "ROLE_SUPER_ADMIN" : "PERM_" + required.name();
        if (!auth.getAuthorities().contains(new SimpleGrantedAuthority(requiredAuthority))) {
            return denyAccess(response);
        }

        return true;
    }

    private static boolean denyAccess(HttpServletResponse response) throws IOException {
        response.sendRedirect("/admin/access-denied");
        return false;
    }
}
