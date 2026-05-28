package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
                       Model model) {
        model.addAttribute("logs", adminLogService.getLogs(page, PAGE_SIZE, targetType));
        model.addAttribute("targetType", targetType);
        model.addAttribute("extraParams", targetType.isBlank() ? null : "targetType=" + targetType);
        return "admin/admin-logs";
    }
}
