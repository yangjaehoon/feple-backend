package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
public class StatsAdminController {

    private final AdminStatsService adminStatsService;

    @GetMapping
    public String stats(Model model) {
        model.addAttribute("activityStats", adminStatsService.getUserActivityStats());
        model.addAttribute("monthlyStats", adminStatsService.getMonthlyStats());
        model.addAttribute("contentTrend", adminStatsService.getContentTrend());
        return "admin/stats";
    }
}
