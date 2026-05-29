package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.service.AdminMetricsService;
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

    private static final int DEFAULT_RANGE_DAYS = 30;

    private final AdminMetricsService adminStatsService;

    @GetMapping
    public String stats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        LocalDate[] range = normalizeDateRange(from, to);
        model.addAttribute("activityStats", adminStatsService.getUserActivityStats());
        model.addAttribute("rangeStats", adminStatsService.getRangeStats(range[0], range[1]));
        model.addAttribute("contentTrend", adminStatsService.getContentTrend());
        model.addAttribute("from", range[0].toString());
        model.addAttribute("to", range[1].toString());
        return "admin/stats";
    }

    private static LocalDate[] normalizeDateRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate end   = (to == null || to.isAfter(today)) ? today : to;
        LocalDate start = (from == null) ? end.minusDays(DEFAULT_RANGE_DAYS - 1) : from;
        if (start.isAfter(end)) start = end;
        return new LocalDate[]{ start, end };
    }
}
