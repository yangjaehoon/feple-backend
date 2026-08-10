package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.admin.account.AdminAccountService;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final AdminDashboardAssembler dashboardAssembler;
    private final AdminLogService adminLogService;
    private final AdminAccountService adminAccountService;

    @GetMapping
    public String adminHome(Authentication authentication, Model model) {
        model.addAttribute("dashboard", dashboardAssembler.assemble());
        // /admin은 AdminPermissionInterceptor의 권한 체크를 우회하는 경로(OPEN_TO_ANY_ADMIN)이므로,
        // 감사로그의 상세 내용(밴 사유·포인트 지급 사유·로그인 실패 시도 등)은 /admin/logs와 동일하게
        // LOGS 권한을 가진 관리자에게만 노출한다.
        model.addAttribute("recentLogs", hasLogsPermission(authentication) ? adminLogService.getRecentLogs() : List.of());
        model.addAttribute("actionLabels", AdminAction.actionLabelMap());
        adminAccountService.findByUsername(authentication.getName())
                .ifPresent(admin -> model.addAttribute("currentAdmin", admin));
        return "admin/dashboard/home";
    }

    private boolean hasLogsPermission(Authentication authentication) {
        String requiredAuthority = "PERM_" + AdminPermission.LOGS.name();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(requiredAuthority));
    }
}
