package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.service.PhotoPresignedUrlProvider;
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

    private final Map<String, ReportAdminService<?>> handlers;
    private final AdminLogService adminLogService;

    public ReportAdminController(List<ReportAdminService<?>> services, AdminLogService adminLogService) {
        this.adminLogService = adminLogService;
        this.handlers = services.stream()
                .collect(Collectors.toMap(ReportAdminService::getReportType, s -> s));
    }

    @GetMapping
    public String list(@ModelAttribute ReportFilter filter, Model model) {
        ReportAdminService<?> handler = resolveHandler(filter.type());
        Page<?> reports = handler.searchReportsForAdmin(new ReportSearchParams(filter.page(), AdminConstants.LIST_PAGE_SIZE, filter.status(), filter.keyword()));
        model.addAttribute("reports", reports);
        model.addAttribute("pendingCount", handler.getPendingCount());
        model.addAttribute("totalCount", handler.getTotalCount());
        model.addAttribute("status",  filter.status());
        model.addAttribute("type",    filter.type());
        model.addAttribute("keyword", filter.keyword());
        model.addAttribute("authorReportCounts", reports.isEmpty() ? Map.of() : buildCounts(handler, reports));
        model.addAttribute("photoUrls", handler instanceof PhotoPresignedUrlProvider provider
                ? provider.buildPhotoPresignedUrls(reports) : Map.of());
        handlers.forEach((t, h) -> model.addAttribute(t + "PendingCount", h.getPendingCount()));
        return "admin/moderation/reports";
    }

    @PostMapping("/{id}/delete")
    public String deleteContent(@PathVariable Long id,
                                @ModelAttribute ReportFilter filter,
                                RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    resolveHandler(filter.type()).deleteContentAndResolve(id);
                    adminLogService.log(AdminAction.REPORT_DELETE, "REPORT", id, filter.type());
                },
                "콘텐츠를 삭제하고 신고를 처리했습니다.",
                e -> log.error("신고 콘텐츠 삭제 실패 id={} type={}", id, filter.type(), e),
                "삭제 처리 중 오류가 발생했습니다.",
                ra);
        return redirectReports(filter);
    }

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable Long id,
                          @ModelAttribute ReportFilter filter,
                          RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    resolveHandler(filter.type()).dismissReport(id);
                    adminLogService.log(AdminAction.REPORT_DISMISS, "REPORT", id, filter.type());
                },
                "신고를 기각했습니다.",
                e -> log.error("신고 기각 실패 id={} type={}", id, filter.type(), e),
                "기각 처리 중 오류가 발생했습니다.",
                ra);
        return redirectReports(filter);
    }

    @PostMapping("/bulk-dismiss")
    public String bulkDismiss(@RequestParam(required = false) List<Long> ids,
                              @ModelAttribute ReportFilter filter,
                              RedirectAttributes ra) {
        String emptySelection = AdminActionUtils.requireNonEmptySelection(ids, redirectReports(filter), ra);
        if (emptySelection != null) return emptySelection;
        AdminActionUtils.tryAction(
                () -> {
                    resolveHandler(filter.type()).bulkDismiss(ids);
                    adminLogService.log(AdminAction.REPORT_BULK_DISMISS, "REPORT", null, filter.type() + " " + ids.size() + "건");
                },
                ids.size() + "건을 일괄 기각했습니다.",
                e -> log.error("신고 일괄 기각 실패 type={} ids={}", filter.type(), ids, e),
                "일괄 기각 처리 중 오류가 발생했습니다.",
                ra);
        return redirectReports(filter);
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam(required = false) List<Long> ids,
                             @ModelAttribute ReportFilter filter,
                             RedirectAttributes ra) {
        String emptySelection = AdminActionUtils.requireNonEmptySelection(ids, redirectReports(filter), ra);
        if (emptySelection != null) return emptySelection;
        AdminActionUtils.tryActionWithResult(
                () -> {
                    int done = resolveHandler(filter.type()).bulkDeleteContent(ids);
                    adminLogService.log(AdminAction.REPORT_BULK_DELETE, "REPORT", null,
                            filter.type() + " " + done + "/" + ids.size() + "건");
                    return done;
                },
                done -> done == ids.size()
                        ? ids.size() + "건의 콘텐츠를 삭제하고 신고를 처리했습니다."
                        : done + "/" + ids.size() + "건 처리 완료 (일부 실패)",
                e -> log.error("신고 일괄 삭제 실패 type={} ids={}", filter.type(), ids, e),
                AdminConstants.MSG_BULK_DELETE_ERROR,
                ra);
        return redirectReports(filter);
    }

    private ReportAdminService<?> resolveHandler(String type) {
        return handlers.getOrDefault(type, handlers.get(AdminConstants.REPORT_TYPE_POST));
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<Long, Long> buildCounts(ReportAdminService<T> handler, Page<?> reports) {
        return handler.buildAuthorReportCounts((Page<T>) reports);
    }

    private String redirectReports(ReportFilter filter) {
        String safeType   = handlers.containsKey(filter.type()) ? filter.type() : AdminConstants.REPORT_TYPE_POST;
        String safeStatus = AdminConstants.STATUS_PENDING.equals(filter.status()) ? AdminConstants.STATUS_PENDING : AdminConstants.STATUS_ALL;
        // 처리 후 총 건수 기준으로 유효 페이지 범위를 재계산해 빈 페이지 착지를 방지
        // keyword 검색 시에는 전체 카운트가 필터 결과를 반영하지 않으므로 조정 생략
        int safePage = filter.page();
        if (filter.keyword().isBlank()) {
            ReportAdminService<?> handler = handlers.get(safeType);
            long newTotal = AdminConstants.STATUS_PENDING.equals(safeStatus)
                    ? handler.getPendingCount()
                    : handler.getTotalCount();
            int maxPage = newTotal > 0 ? (int) ((newTotal - 1) / AdminConstants.LIST_PAGE_SIZE) : 0;
            safePage = Math.min(filter.page(), maxPage);
        }
        return AdminActionUtils.toRedirect(
                UriComponentsBuilder.fromPath("/admin/reports")
                        .queryParam("type", safeType)
                        .queryParam("status", safeStatus)
                        .queryParam("page", safePage),
                filter.keyword());
    }
}
