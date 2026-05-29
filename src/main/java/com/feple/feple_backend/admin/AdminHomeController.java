package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.account.AdminAccountService;
import com.feple.feple_backend.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final AdminDashboardAssembler dashboardAssembler;
    private final AdminLogService adminLogService;
    private final AdminAccountService adminAccountService;

    @GetMapping
    public String adminHome(Authentication authentication,
                            @RequestParam(defaultValue = "0") int festivalPage,
                            @RequestParam(defaultValue = "0") int artistPage,
                            Model model) {
        model.addAttribute("dashboard", dashboardAssembler.assemble(festivalPage, artistPage));
        model.addAttribute("recentLogs", adminLogService.getRecentLogs());
        adminAccountService.findByUsername(authentication.getName())
                .ifPresent(admin -> model.addAttribute("currentAdmin", admin));
        return "admin/admin-home";
    }
}
