package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private static final int PAGE_SIZE = 50;

    private final AdminLogService adminLogService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String targetType,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model) {
        model.addAttribute("logs", adminLogService.getLogs(page, PAGE_SIZE, targetType, from, to));
        model.addAttribute("targetType", targetType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);

        List<String> params = new ArrayList<>();
        if (!targetType.isBlank()) params.add("targetType=" + targetType);
        if (from != null) params.add("from=" + from);
        if (to != null) params.add("to=" + to);
        model.addAttribute("extraParams", params.isEmpty() ? null : String.join("&", params));

        return "admin/admin-logs";
    }
}
