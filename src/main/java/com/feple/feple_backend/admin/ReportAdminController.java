package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.artist.photo.service.ArtistPhotoReportService;
import com.feple.feple_backend.comment.service.CommentReportService;
import com.feple.feple_backend.post.service.PostReportService;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/reports")
public class ReportAdminController {

    private static final int PAGE_SIZE = 20;

    private final PostReportService postReportService;
    private final CommentReportService commentReportService;
    private final ArtistPhotoReportService photoReportService;
    private final AdminLogService adminLogService;
    private final Map<String, ReportAdminService> handlers;

    public ReportAdminController(PostReportService postReportService,
                                  CommentReportService commentReportService,
                                  ArtistPhotoReportService photoReportService,
                                  AdminLogService adminLogService) {
        this.postReportService = postReportService;
        this.commentReportService = commentReportService;
        this.photoReportService = photoReportService;
        this.adminLogService = adminLogService;
        this.handlers = Map.of(
                "post", postReportService,
                "comment", commentReportService,
                "photo", photoReportService);
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "post") String type,
            Model model) {
        ReportAdminService handler = handlers.getOrDefault(type, postReportService);
        Page<?> reports = handler.getReportsForAdmin(page, PAGE_SIZE, status);
        model.addAttribute("reports", reports);
        model.addAttribute("pendingCount", handler.getPendingCount());
        model.addAttribute("totalCount", handler.getTotalCount());
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("authorReportCounts", reports.isEmpty() ? Map.of() : handler.buildAuthorReportCounts(reports));
        model.addAttribute("postPendingCount", postReportService.getPendingCount());
        model.addAttribute("commentPendingCount", commentReportService.getPendingCount());
        model.addAttribute("photoPendingCount", photoReportService.getPendingCount());
        return "admin/report-list";
    }

    @PostMapping("/{id}/delete-post")
    public String deletePost(@PathVariable Long id,
                             @RequestParam(defaultValue = "0") int page,
                             RedirectAttributes ra) {
        postReportService.deletePostAndResolve(id);
        adminLogService.log("REPORT_POST_DELETE", "REPORT", id, null);
        ra.addFlashAttribute("successMessage", "게시글을 삭제하고 신고를 처리했습니다.");
        return "redirect:/admin/reports?type=post&page=" + page;
    }

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable Long id,
                          @RequestParam(defaultValue = "post") String type,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes ra) {
        ReportAdminService handler = handlers.getOrDefault(type, postReportService);
        handler.dismissReport(id);
        adminLogService.log("REPORT_DISMISS", "REPORT", id, type);
        ra.addFlashAttribute("successMessage", "신고를 기각했습니다.");
        return "redirect:/admin/reports?type=" + type + "&page=" + page;
    }

    @PostMapping("/comments/{id}/delete-comment")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam(defaultValue = "0") int page,
                                RedirectAttributes ra) {
        commentReportService.deleteCommentAndResolve(id);
        adminLogService.log("REPORT_COMMENT_DELETE", "REPORT", id, null);
        ra.addFlashAttribute("successMessage", "댓글을 삭제하고 신고를 처리했습니다.");
        return "redirect:/admin/reports?type=comment&page=" + page;
    }

    @PostMapping("/photos/{id}/delete-photo")
    public String deletePhoto(@PathVariable Long id,
                              @RequestParam(defaultValue = "0") int page,
                              RedirectAttributes ra) {
        photoReportService.deletePhotoAndResolve(id);
        adminLogService.log("REPORT_PHOTO_DELETE", "REPORT", id, null);
        ra.addFlashAttribute("successMessage", "사진을 삭제하고 신고를 처리했습니다.");
        return "redirect:/admin/reports?type=photo&page=" + page;
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
        ReportAdminService handler = handlers.getOrDefault(type, postReportService);
        handler.bulkDismiss(ids);
        adminLogService.log("REPORT_BULK_DISMISS", "REPORT", null, type + " " + ids.size() + "건");
        ra.addFlashAttribute("successMessage", ids.size() + "건을 일괄 기각했습니다.");
        return redirectReports(type, status, page);
    }

    @PostMapping("/bulk-delete-post")
    public String bulkDeletePost(@RequestParam List<Long> ids,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "PENDING") String status,
                                 RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "선택된 항목이 없습니다.");
            return redirectReports("post", status, page);
        }
        int count = 0;
        for (Long id : ids) {
            try {
                postReportService.deletePostAndResolve(id);
                count++;
            } catch (Exception ignored) {}
        }
        adminLogService.log("REPORT_BULK_POST_DELETE", "REPORT", null, count + "건");
        ra.addFlashAttribute("successMessage", count + "건의 게시글을 삭제하고 신고를 처리했습니다.");
        return redirectReports("post", status, page);
    }

    @PostMapping("/bulk-delete-comment")
    public String bulkDeleteComment(@RequestParam List<Long> ids,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "PENDING") String status,
                                    RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "선택된 항목이 없습니다.");
            return redirectReports("comment", status, page);
        }
        int count = 0;
        for (Long id : ids) {
            try {
                commentReportService.deleteCommentAndResolve(id);
                count++;
            } catch (Exception ignored) {}
        }
        adminLogService.log("REPORT_BULK_COMMENT_DELETE", "REPORT", null, count + "건");
        ra.addFlashAttribute("successMessage", count + "건의 댓글을 삭제하고 신고를 처리했습니다.");
        return redirectReports("comment", status, page);
    }

    @PostMapping("/bulk-delete-photo")
    public String bulkDeletePhoto(@RequestParam List<Long> ids,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "PENDING") String status,
                                  RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "선택된 항목이 없습니다.");
            return redirectReports("photo", status, page);
        }
        int count = 0;
        for (Long id : ids) {
            try {
                photoReportService.deletePhotoAndResolve(id);
                count++;
            } catch (Exception ignored) {}
        }
        adminLogService.log("REPORT_BULK_PHOTO_DELETE", "REPORT", null, count + "건");
        ra.addFlashAttribute("successMessage", count + "건의 사진을 삭제하고 신고를 처리했습니다.");
        return redirectReports("photo", status, page);
    }

    private String redirectReports(String type, String status, int page) {
        return "redirect:/admin/reports?type=" + type + "&status=" + status + "&page=" + page;
    }
}
