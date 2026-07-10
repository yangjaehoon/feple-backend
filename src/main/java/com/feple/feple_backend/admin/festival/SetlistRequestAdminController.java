package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.festival.setlistrequest.entity.SetlistChangeRequestStatus;
import com.feple.feple_backend.festival.setlistrequest.service.SetlistChangeRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/setlist-requests")
@RequiredArgsConstructor
public class SetlistRequestAdminController {

    private final SetlistChangeRequestService service;
    private final AdminLogService adminLogService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        SetlistChangeRequestStatus statusEnum = parseStatus(status);
        var requests = service.list(statusEnum, keyword, PageRequest.of(page, AdminConstants.LIST_PAGE_SIZE));
        model.addAttribute("requests", requests);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pendingCount", service.countPending());
        return "admin/setlist-request/list";
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id,
                          @RequestParam(defaultValue = "PENDING") String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "") String keyword,
                          RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    service.resolve(id);
                    adminLogService.log(AdminAction.SETLIST_REQUEST_RESOLVE, "SETLIST_REQUEST", id, null);
                },
                "처리 완료로 표시했습니다.",
                e -> log.error("셋리스트 요청 처리 실패: id={}", id, e),
                "처리 중 오류가 발생했습니다.",
                ra);

        long remaining = service.countByStatus(parseStatus(status));
        int maxPage = remaining > 0 ? (int) ((remaining - 1) / AdminConstants.LIST_PAGE_SIZE) : 0;
        int safePage = Math.min(page, maxPage);
        String redirect = "redirect:/admin/setlist-requests?status=" + status + "&page=" + safePage;
        if (!keyword.isBlank()) redirect += "&keyword=" + keyword;
        return redirect;
    }

    private SetlistChangeRequestStatus parseStatus(String status) {
        try {
            return SetlistChangeRequestStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return SetlistChangeRequestStatus.PENDING;
        }
    }
}
