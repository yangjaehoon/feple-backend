package com.feple.feple_backend.admin;

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

import java.util.Map;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/reports")
public class ReportAdminController {

    private final PostReportService postReportService;
    private final CommentReportService commentReportService;
    private final ArtistPhotoReportService photoReportService;
    private final Map<String, ReportAdminService> handlers;

    public ReportAdminController(PostReportService postReportService,
                                  CommentReportService commentReportService,
                                  ArtistPhotoReportService photoReportService) {
        this.postReportService = postReportService;
        this.commentReportService = commentReportService;
        this.photoReportService = photoReportService;
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
        Page<?> reports = handler.getReportsForAdmin(page, 20, status);
        model.addAttribute("reports", reports);
        model.addAttribute("pendingCount", handler.getPendingCount());
        model.addAttribute("totalCount", handler.getTotalCount());
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("authorReportCounts", reports.isEmpty() ? Map.of() : handler.buildAuthorReportCounts(reports));
        return "admin/report-list";
    }

    @PostMapping("/{id}/delete-post")
    public String deletePost(@PathVariable Long id,
                             @RequestParam(defaultValue = "0") int page,
                             RedirectAttributes ra) {
        postReportService.deletePostAndResolve(id);
        ra.addFlashAttribute("successMessage", "게시글을 삭제하고 신고를 처리했습니다.");
        return "redirect:/admin/reports?type=post&page=" + page;
    }

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable Long id,
                          @RequestParam(defaultValue = "post") String type,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes ra) {
        handlers.getOrDefault(type, postReportService).dismissReport(id);
        ra.addFlashAttribute("successMessage", "신고를 기각했습니다.");
        return "redirect:/admin/reports?type=" + type + "&page=" + page;
    }

    @PostMapping("/comments/{id}/delete-comment")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam(defaultValue = "0") int page,
                                RedirectAttributes ra) {
        commentReportService.deleteCommentAndResolve(id);
        ra.addFlashAttribute("successMessage", "댓글을 삭제하고 신고를 처리했습니다.");
        return "redirect:/admin/reports?type=comment&page=" + page;
    }

    @PostMapping("/photos/{id}/delete-photo")
    public String deletePhoto(@PathVariable Long id,
                              @RequestParam(defaultValue = "0") int page,
                              RedirectAttributes ra) {
        photoReportService.deletePhotoAndResolve(id);
        ra.addFlashAttribute("successMessage", "사진을 삭제하고 신고를 처리했습니다.");
        return "redirect:/admin/reports?type=photo&page=" + page;
    }
}
