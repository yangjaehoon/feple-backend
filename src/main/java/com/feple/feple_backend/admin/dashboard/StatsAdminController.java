package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.admin.service.AdminStatsMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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

    private static final int DEFAULT_RANGE_DAYS = 30;
    private static final int MAX_RANGE_DAYS     = 90;

    private final AdminStatsMetrics adminStatsService;

    @GetMapping
    @Transactional(readOnly = true)
    public String stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        LocalDate[] range = clampDateRange(from, to);
        LocalDate start   = range[0];
        LocalDate end     = range[1];

        model.addAttribute("activityStats", adminStatsService.getUserActivityStats());
        model.addAttribute("rangeStats", adminStatsService.getRangeStats(start, end));
        model.addAttribute("contentTrend", adminStatsService.getContentTrend());
        model.addAttribute("from", start.toString());
        model.addAttribute("to", end.toString());
        return "admin/dashboard/stats";
    }

    private static LocalDate[] clampDateRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate end   = (to == null || to.isAfter(today)) ? today : to;
        LocalDate start = (from == null) ? end.minusDays(DEFAULT_RANGE_DAYS - 1) : from;
        if (start.isAfter(end))                              start = end;
        if (start.isBefore(end.minusDays(MAX_RANGE_DAYS - 1))) start = end.minusDays(MAX_RANGE_DAYS - 1);
        return new LocalDate[]{start, end};
    }
}
