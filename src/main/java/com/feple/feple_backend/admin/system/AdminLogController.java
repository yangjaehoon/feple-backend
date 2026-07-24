package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogFilter;
import com.feple.feple_backend.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                       @ModelAttribute AdminLogFilter filter,
                       Model model) {
        model.addAttribute("logs", adminLogService.getLogs(page, filter));
        model.addAttribute("targetType", filter.targetType());
        model.addAttribute("adminUsername", filter.adminUsername());
        model.addAttribute("from", filter.from());
        model.addAttribute("to", filter.to());
        model.addAttribute("actionLabels", AdminAction.actionLabelMap());

        List<String> params = new ArrayList<>();
        if (!filter.targetType().isBlank()) params.add("targetType=" + URLEncoder.encode(filter.targetType(), StandardCharsets.UTF_8));
        if (!filter.adminUsername().isBlank()) params.add("adminUsername=" + URLEncoder.encode(filter.adminUsername(), StandardCharsets.UTF_8));
        if (filter.from() != null) params.add("from=" + filter.from());
        if (filter.to() != null) params.add("to=" + filter.to());
        model.addAttribute("extraParams", params.isEmpty() ? null : String.join("&", params));

        return "admin/system/logs";
    }
}
