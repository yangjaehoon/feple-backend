package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.service.S3PresignService;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/certifications")
@RequiredArgsConstructor
public class CertificationAdminController {

    private final FestivalCertificationService certificationService;
    private final S3PresignService s3PresignService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            Model model) {

        CertificationStatus filterStatus = null;
        if (status != null && !status.isEmpty()) {
            filterStatus = CertificationStatus.valueOf(status);
        }

        Page<FestivalCertification> certPage = certificationService.getByStatus(
                filterStatus,
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("certifications", certPage);
        model.addAttribute("status", status != null ? status : "");
        model.addAttribute("page", page);
        return "admin/certification-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        FestivalCertification cert = certificationService.getById(id);
        String photoUrl = s3PresignService.presignGetUrl(cert.getPhotoKey());

        model.addAttribute("cert", cert);
        model.addAttribute("photoUrl", photoUrl);
        return "admin/certification-detail";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(defaultValue = "") String status,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes ra) {
        certificationService.approve(id, "admin");
        ra.addFlashAttribute("successMessage", "인증이 승인되었습니다.");
        return "redirect:/admin/certifications?status=" + status + "&page=" + page;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String rejectionMessage,
                         @RequestParam(defaultValue = "") String status,
                         @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes ra) {
        certificationService.reject(id, rejectionMessage, "admin");
        ra.addFlashAttribute("successMessage", "인증이 거절되었습니다.");
        return "redirect:/admin/certifications?status=" + status + "&page=" + page;
    }
}
