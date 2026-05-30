package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final AdminLogService adminLogService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) CertificationStatus status,
            @RequestParam(required = false) String keyword,
            Model model) {

        Page<FestivalCertification> certPage = (keyword != null && !keyword.isBlank())
                ? certificationService.searchByKeyword(keyword, status, page)
                : certificationService.getByStatus(status, page);

        model.addAttribute("certifications", certPage);
        model.addAttribute("status", status != null ? status.name() : "");
        model.addAttribute("keyword", keyword);
        model.addAttribute("page", page);
        return "admin/certification-list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        FestivalCertification cert = certificationService.getById(id);
        String photoUrl = certificationService.buildPhotoUrl(cert.getPhotoKey());

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
        adminLogService.log("CERTIFICATION_APPROVE", "CERTIFICATION", id, null);
        ra.addFlashAttribute("successMessage", "인증이 승인되었습니다.");
        return certRedirect(status, page);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String rejectionMessage,
                         @RequestParam(defaultValue = "") String status,
                         @RequestParam(defaultValue = "0") int page,
                         RedirectAttributes ra) {
        certificationService.reject(id, rejectionMessage, "admin");
        adminLogService.log("CERTIFICATION_REJECT", "CERTIFICATION", id,
                rejectionMessage.isBlank() ? null : rejectionMessage);
        ra.addFlashAttribute("successMessage", "인증이 거절되었습니다.");
        return certRedirect(status, page);
    }

    private String certRedirect(String status, int page) {
        return "redirect:/admin/certifications?status=" + status + "&page=" + page;
    }
}
