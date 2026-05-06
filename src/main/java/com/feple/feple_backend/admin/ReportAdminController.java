package com.feple.feple_backend.admin;

import com.feple.feple_backend.post.service.PostReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class ReportAdminController {

    private final PostReportService postReportService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "PENDING") String status,
            Model model) {
        model.addAttribute("reports", postReportService.getReportsForAdmin(page, 20, status));
        model.addAttribute("status", status);
        model.addAttribute("pendingCount", postReportService.getPendingCount());
        model.addAttribute("totalCount", postReportService.getTotalCount());
        return "admin/report-list";
    }

    @PostMapping("/{id}/delete-post")
    public String deletePost(@PathVariable Long id,
                             @RequestParam(defaultValue = "0") int page,
                             RedirectAttributes ra) {
        postReportService.deletePostAndResolve(id);
        ra.addFlashAttribute("successMessage", "게시글을 삭제하고 신고를 처리했습니다.");
        return "redirect:/admin/reports?page=" + page;
    }

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable Long id,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes ra) {
        postReportService.dismissReport(id);
        ra.addFlashAttribute("successMessage", "신고를 기각했습니다.");
        return "redirect:/admin/reports?page=" + page;
    }
}
