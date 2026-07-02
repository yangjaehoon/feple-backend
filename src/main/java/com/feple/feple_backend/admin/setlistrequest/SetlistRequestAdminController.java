package com.feple.feple_backend.admin.setlistrequest;

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

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        SetlistChangeRequestStatus statusEnum = parseStatus(status);
        var requests = service.list(statusEnum, PageRequest.of(page, 20));
        model.addAttribute("requests", requests);
        model.addAttribute("status", status);
        model.addAttribute("pendingCount", service.countPending());
        return "admin/setlist-request/list";
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.resolve(id);
            ra.addFlashAttribute("successMessage", "처리 완료로 표시했습니다.");
        } catch (Exception e) {
            log.error("셋리스트 요청 처리 실패: id={}", id, e);
            ra.addFlashAttribute("errorMessage", "처리 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/setlist-requests";
    }

    private SetlistChangeRequestStatus parseStatus(String status) {
        try {
            return SetlistChangeRequestStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return SetlistChangeRequestStatus.PENDING;
        }
    }
}
