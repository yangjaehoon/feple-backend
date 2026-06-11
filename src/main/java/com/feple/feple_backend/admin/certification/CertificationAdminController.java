package com.feple.feple_backend.admin.certification;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.NoSuchElementException;

@Slf4j
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
        model.addAttribute("pendingCount", certificationService.getPendingCount());
        return "admin/certification/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            FestivalCertification cert = certificationService.getById(id);
            String photoUrl = certificationService.buildPhotoUrl(cert.getPhotoKey());
            model.addAttribute("cert", cert);
            model.addAttribute("photoUrl", photoUrl);
            return "admin/certification/detail";
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/certifications";
        } catch (Exception e) {
            log.error("인증 상세 조회 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "인증 정보를 불러오지 못했습니다.");
            return "redirect:/admin/certifications";
        }
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(defaultValue = "") String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "") String keyword,
                          RedirectAttributes ra) {
        try {
            certificationService.approve(id, "admin");
            adminLogService.log(AdminAction.CERTIFICATION_APPROVE, "CERTIFICATION", id, null);
            ra.addFlashAttribute("successMessage", "인증이 승인되었습니다.");
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("인증 승인 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "승인 처리 중 오류가 발생했습니다.");
        }
        return certRedirect(status, page, keyword);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String rejectionMessage,
                         @RequestParam(defaultValue = "") String status,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "") String keyword,
                         RedirectAttributes ra) {
        try {
            certificationService.reject(id, rejectionMessage, "admin");
            adminLogService.log(AdminAction.CERTIFICATION_REJECT, "CERTIFICATION", id,
                    rejectionMessage.isBlank() ? null : rejectionMessage);
            ra.addFlashAttribute("successMessage", "인증이 거절되었습니다.");
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("인증 거절 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "거절 처리 중 오류가 발생했습니다.");
        }
        return certRedirect(status, page, keyword);
    }

    private String certRedirect(String status, int page, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/certifications")
                .queryParam("status", status)
                .queryParam("page", page);
        if (keyword != null && !keyword.isBlank()) builder.queryParam("keyword", keyword);
        return "redirect:" + builder.build().toUriString();
    }
}
