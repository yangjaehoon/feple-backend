package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.service.ReportAdminService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/reports")
public class ReportAdminController {

    private static final int PAGE_SIZE = 20;

    private final Map<String, ReportAdminService> handlers;
    private final AdminLogService adminLogService;

    public ReportAdminController(List<ReportAdminService> services, AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
        this.handlers = services.stream()
                .collect(Collectors.toMap(ReportAdminService::getReportType, s -> s));
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "post") String type,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        ReportAdminService handler = resolveHandler(type);
        Page<?> reports = keyword.isBlank()
                ? handler.getReportsForAdmin(page, PAGE_SIZE, status)
                : handler.searchReportsForAdmin(page, PAGE_SIZE, status, keyword);
        model.addAttribute("reports", reports);
        model.addAttribute("pendingCount", handler.getPendingCount());
        model.addAttribute("totalCount", handler.getTotalCount());
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("keyword", keyword);
        model.addAttribute("authorReportCounts", reports.isEmpty() ? Map.of() : handler.buildAuthorReportCounts(reports));
        handlers.forEach((t, h) -> model.addAttribute(t + "PendingCount", h.getPendingCount()));
        return "admin/report-list";
    }

    @PostMapping("/{id}/delete")
    public String deleteContent(@PathVariable Long id,
                                @RequestParam(defaultValue = "post") String type,
                                @RequestParam(defaultValue = "0") int page,
                                RedirectAttributes ra) {
        resolveHandler(type).deleteContentAndResolve(id);
        adminLogService.log("REPORT_DELETE", "REPORT", id, type);
        ra.addFlashAttribute("successMessage", "콘텐츠를 삭제하고 신고를 처리했습니다.");
        return redirectReports(type, "PENDING", page);
    }

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable Long id,
                          @RequestParam(defaultValue = "post") String type,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes ra) {
        resolveHandler(type).dismissReport(id);
        adminLogService.log("REPORT_DISMISS", "REPORT", id, type);
        ra.addFlashAttribute("successMessage", "신고를 기각했습니다.");
        return redirectReports(type, "PENDING", page);
    }

    @PostMapping("/bulk-dismiss")
    public String bulkDismiss(@RequestParam List<Long> ids,
                              @RequestParam(defaultValue = "post") String type,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "PENDING") String status,
                              RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "선택된 항목이 없습니다.");
            return redirectReports(type, status, page);
        }
        resolveHandler(type).bulkDismiss(ids);
        adminLogService.log("REPORT_BULK_DISMISS", "REPORT", null, type + " " + ids.size() + "건");
        ra.addFlashAttribute("successMessage", ids.size() + "건을 일괄 기각했습니다.");
        return redirectReports(type, status, page);
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam List<Long> ids,
                             @RequestParam(defaultValue = "post") String type,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "PENDING") String status,
                             RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "선택된 항목이 없습니다.");
            return redirectReports(type, status, page);
        }
        resolveHandler(type).bulkDeleteContent(ids);
        adminLogService.log("REPORT_BULK_DELETE", "REPORT", null, type + " " + ids.size() + "건");
        ra.addFlashAttribute("successMessage", ids.size() + "건의 콘텐츠를 삭제하고 신고를 처리했습니다.");
        return redirectReports(type, status, page);
    }

    private ReportAdminService resolveHandler(String type) {
        return handlers.getOrDefault(type, handlers.values().iterator().next());
    }

    private String redirectReports(String type, String status, int page) {
        return "redirect:/admin/reports?type=" + type + "&status=" + status + "&page=" + page;
    }
}
