package com.feple.feple_backend.admin.search;

import com.feple.feple_backend.admin.account.AdminPermission;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 여러 도메인(PERM_USERS/POSTS/ARTISTS/FESTIVALS)을 넘나드는 전역 검색이라 특정
// @RequiresAdminPermission으로 묶을 수 없다 — 대시보드 루트("/admin")와 동일하게
// AdminPermissionInterceptor/AdminPermissionAnnotationValidator의 예외 경로로 등록돼 있고,
// 대신 도메인별 결과는 AdminGlobalSearchService가 호출자의 PERM_* 권한을 직접 확인해 걸러낸다.
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/search")
@RequiredArgsConstructor
public class AdminGlobalSearchController {

    private final AdminGlobalSearchService searchService;

    @GetMapping
    public String search(@RequestParam(defaultValue = "") String keyword,
                         Authentication authentication, Model model) {
        model.addAttribute("keyword", keyword);
        if (!keyword.isBlank()) {
            model.addAttribute("results", searchService.search(keyword, grantedPermissions(authentication)));
        }
        return "admin/search/results";
    }

    private static Set<AdminPermission> grantedPermissions(Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return Arrays.stream(AdminPermission.values())
                .filter(permission -> authorities.contains("PERM_" + permission.name()))
                .collect(Collectors.toSet());
    }
}
