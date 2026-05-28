package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/stats")
public class StatsAdminController {

    private final AdminStatsService adminStatsService;

    @GetMapping
    public String stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        LocalDate today = LocalDate.now();
        if (to == null || to.isAfter(today)) to = today;
        if (from == null) from = to.minusDays(29);
        if (from.isAfter(to)) from = to;

        model.addAttribute("activityStats", adminStatsService.getUserActivityStats());
        model.addAttribute("rangeStats", adminStatsService.getRangeStats(from, to));
        model.addAttribute("contentTrend", adminStatsService.getContentTrend());
        model.addAttribute("from", from.toString());
        model.addAttribute("to", to.toString());
        return "admin/stats";
    }
}
