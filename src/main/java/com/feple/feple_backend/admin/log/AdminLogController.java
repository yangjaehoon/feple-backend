package com.feple.feple_backend.admin.log;

import com.feple.feple_backend.admin.AdminUrlUtils;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.LOGS)
@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping
    public String list(@ModelAttribute AdminLogFilter filter, Model model) {
        model.addAttribute("logs", adminLogService.getLogs(filter.page(), filter));
        model.addAttribute("targetType", filter.targetType());
        model.addAttribute("adminUsername", filter.adminUsername());
        model.addAttribute("from", filter.from());
        model.addAttribute("to", filter.to());
        model.addAttribute("actionLabels", AdminAction.actionLabelMap());

        String extraParams = AdminUrlUtils.buildQueryString(
                "targetType", filter.targetType(),
                "adminUsername", filter.adminUsername(),
                "from", filter.from(),
                "to", filter.to());
        model.addAttribute("extraParams", extraParams.isEmpty() ? null : extraParams);

        return "admin/system/logs";
    }
}
