package com.feple.feple_backend.admin;

import com.feple.feple_backend.post.service.PostReportService;
import com.feple.feple_backend.comment.service.CommentReportService;
import com.feple.feple_backend.artist.photo.service.ArtistPhotoReportService;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.feple.feple_backend.admin")
@RequiredArgsConstructor
public class AdminSidebarAdvice {

    private final PostReportService postReportService;
    private final CommentReportService commentReportService;
    private final ArtistPhotoReportService photoReportService;
    private final FestivalCertificationService certificationService;

    @ModelAttribute("sidebarReportCount")
    public long sidebarReportCount() {
        try {
            return postReportService.getPendingCount()
                    + commentReportService.getPendingCount()
                    + photoReportService.getPendingCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarCertCount")
    public long sidebarCertCount() {
        try {
            return certificationService.getPendingCount();
        } catch (Exception e) {
            return 0;
        }
    }
}
