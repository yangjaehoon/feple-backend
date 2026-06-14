package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogFilter;
import com.feple.feple_backend.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String targetType,
                       @RequestParam(defaultValue = "") String adminUsername,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model) {
        model.addAttribute("logs", adminLogService.getLogs(page, new AdminLogFilter(targetType, adminUsername, from, to)));
        model.addAttribute("targetType", targetType);
        model.addAttribute("adminUsername", adminUsername);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("actionLabels", AdminAction.actionLabelMap());

        List<String> params = new ArrayList<>();
        if (!targetType.isBlank()) params.add("targetType=" + targetType);
        if (!adminUsername.isBlank()) params.add("adminUsername=" + URLEncoder.encode(adminUsername, StandardCharsets.UTF_8));
        if (from != null) params.add("from=" + from);
        if (to != null) params.add("to=" + to);
        model.addAttribute("extraParams", params.isEmpty() ? null : String.join("&", params));

        return "admin/system/logs";
    }
}
