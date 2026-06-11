package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.admin.service.ReportSearchParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/reports")
public class ReportAdminController {

    private static final int PAGE_SIZE = 20;

    private final Map<String, ReportAdminService<?>> handlers;
    private final AdminLogService adminLogService;

    public ReportAdminController(List<ReportAdminService<?>> services, AdminLogService adminLogService) {
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
        ReportAdminService<?> handler = resolveHandler(type);
        Page<?> reports = handler.searchReportsForAdmin(new ReportSearchParams(page, PAGE_SIZE, status, keyword));
        model.addAttribute("reports", reports);
        model.addAttribute("pendingCount", handler.getPendingCount());
        model.addAttribute("totalCount", handler.getTotalCount());
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("keyword", keyword);
        model.addAttribute("authorReportCounts", reports.isEmpty() ? Map.of() : buildCounts(handler, reports));
        handlers.forEach((t, h) -> model.addAttribute(t + "PendingCount", h.getPendingCount()));
        return "admin/moderation/reports";
    }

    @PostMapping("/{id}/delete")
    public String deleteContent(@PathVariable Long id,
                                @RequestParam(defaultValue = "post") String type,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "PENDING") String status,
                                @RequestParam(defaultValue = "") String keyword,
                                RedirectAttributes ra) {
        try {
            resolveHandler(type).deleteContentAndResolve(id);
            adminLogService.log("REPORT_DELETE", "REPORT", id, type);
            ra.addFlashAttribute("successMessage", "콘텐츠를 삭제하고 신고를 처리했습니다.");
        } catch (Exception e) {
            log.error("신고 콘텐츠 삭제 실패 id={} type={}", id, type, e);
            ra.addFlashAttribute("errorMessage", "삭제 처리 중 오류가 발생했습니다.");
        }
        return redirectReports(type, status, page, keyword);
    }

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable Long id,
                          @RequestParam(defaultValue = "post") String type,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "PENDING") String status,
                          @RequestParam(defaultValue = "") String keyword,
                          RedirectAttributes ra) {
        try {
            resolveHandler(type).dismissReport(id);
            adminLogService.log("REPORT_DISMISS", "REPORT", id, type);
            ra.addFlashAttribute("successMessage", "신고를 기각했습니다.");
        } catch (Exception e) {
            log.error("신고 기각 실패 id={} type={}", id, type, e);
            ra.addFlashAttribute("errorMessage", "기각 처리 중 오류가 발생했습니다.");
        }
        return redirectReports(type, status, page, keyword);
    }

    @PostMapping("/bulk-dismiss")
    public String bulkDismiss(@RequestParam List<Long> ids,
                              @RequestParam(defaultValue = "post") String type,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "PENDING") String status,
                              @RequestParam(defaultValue = "") String keyword,
                              RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) return emptySelectionRedirect(type, status, page, keyword, ra);
        try {
            resolveHandler(type).bulkDismiss(ids);
            adminLogService.log("REPORT_BULK_DISMISS", "REPORT", null, type + " " + ids.size() + "건");
            ra.addFlashAttribute("successMessage", ids.size() + "건을 일괄 기각했습니다.");
        } catch (Exception e) {
            log.error("신고 일괄 기각 실패 type={} ids={}", type, ids, e);
            ra.addFlashAttribute("errorMessage", "일괄 기각 처리 중 오류가 발생했습니다.");
        }
        return redirectReports(type, status, page, keyword);
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam List<Long> ids,
                             @RequestParam(defaultValue = "post") String type,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "PENDING") String status,
                             @RequestParam(defaultValue = "") String keyword,
                             RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) return emptySelectionRedirect(type, status, page, keyword, ra);
        try {
            resolveHandler(type).bulkDeleteContent(ids);
            adminLogService.log("REPORT_BULK_DELETE", "REPORT", null, type + " " + ids.size() + "건");
            ra.addFlashAttribute("successMessage", ids.size() + "건의 콘텐츠를 삭제하고 신고를 처리했습니다.");
        } catch (Exception e) {
            log.error("신고 일괄 삭제 실패 type={} ids={}", type, ids, e);
            ra.addFlashAttribute("errorMessage", "일괄 삭제 처리 중 오류가 발생했습니다.");
        }
        return redirectReports(type, status, page, keyword);
    }

    private ReportAdminService<?> resolveHandler(String type) {
        ReportAdminService<?> handler = handlers.get(type);
        return handler != null ? handler : handlers.get("post");
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<Long, Long> buildCounts(ReportAdminService<T> handler, Page<?> reports) {
        return handler.buildAuthorReportCounts((Page<T>) reports);
    }

    private String emptySelectionRedirect(String type, String status, int page, String keyword, RedirectAttributes ra) {
        ra.addFlashAttribute("errorMessage", "선택된 항목이 없습니다.");
        return redirectReports(type, status, page, keyword);
    }

    private String redirectReports(String type, String status, int page, String keyword) {
        String safeType   = handlers.containsKey(type) ? type : "post";
        String safeStatus = "PENDING".equals(status) ? "PENDING" : "ALL";
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/reports")
                .queryParam("type", safeType)
                .queryParam("status", safeStatus)
                .queryParam("page", page);
        if (keyword != null && !keyword.isBlank()) builder.queryParam("keyword", keyword);
        return "redirect:" + builder.build().toUriString();
    }
}
