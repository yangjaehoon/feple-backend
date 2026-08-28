package com.feple.feple_backend.admin.point;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.user.dto.PointLogResponseDto;
import com.feple.feple_backend.user.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.USERS)
@Controller
@RequestMapping("/admin/points")
@RequiredArgsConstructor
public class PointAdminController {

    private final PointService pointService;

    @GetMapping
    public String list(@ModelAttribute PointListParams params, Model model) {
        Page<PointLogResponseDto> logs =
                pointService.getAllPointLogs(params.page(), AdminConstants.LIST_PAGE_SIZE, params.keyword());
        model.addAttribute("logs", logs);
        model.addAttribute("keyword", params.keyword());
        return "admin/point/list";
    }
}
