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
import org.springframework.web.bind.annotation.RequestParam;

@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.LOGS)
@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @ModelAttribute AdminLogFilter filter,
                       Model model) {
        // 음수 page는 PageRequest.of()가 IllegalArgumentException을 던져 관리자 화면 대신
        // GlobalExceptionHandler의 raw JSON 에러로 빠지므로, URL을 직접 수정한 경우를 방어한다.
        page = Math.max(0, page);
        model.addAttribute("logs", adminLogService.getLogs(page, filter));
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
