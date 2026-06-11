package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.admin.AdminDashboardAssembler;
import com.feple.feple_backend.admin.account.AdminAccountService;
import com.feple.feple_backend.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional(readOnly = true)
    public String adminHome(Authentication authentication, Model model) {
        model.addAttribute("dashboard", dashboardAssembler.assemble());
        model.addAttribute("recentLogs", adminLogService.getRecentLogs());
        adminAccountService.findByUsername(authentication.getName())
                .ifPresent(admin -> model.addAttribute("currentAdmin", admin));
        return "admin/dashboard/home";
    }
}
