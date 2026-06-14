package com.feple.feple_backend.admin.certification;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/certifications")
@RequiredArgsConstructor
public class CertificationAdminController {

    private final FestivalCertificationService certificationService;
    private final AdminLogService adminLogService;

    @GetMapping
    @Transactional(readOnly = true)
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
        return AdminActionUtils.tryRender(
                () -> {
                    FestivalCertification cert = certificationService.getById(id);
                    model.addAttribute("cert", cert);
                    model.addAttribute("photoUrl", certificationService.buildPhotoUrl(cert.getPhotoKey()));
                },
                "admin/certification/detail",
                e -> log.error("인증 상세 조회 실패 id={}", id, e),
                "인증 정보를 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/certifications",
                ra);
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @ModelAttribute CertFilter filter,
                          Authentication auth,
                          RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    certificationService.approve(id, auth.getName());
                    adminLogService.log(AdminAction.CERTIFICATION_APPROVE, "CERTIFICATION", id, null);
                },
                "인증이 승인되었습니다.",
                e -> log.error("인증 승인 실패 id={}", id, e),
                "승인 처리 중 오류가 발생했습니다.",
                ra);
        return AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword());
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String rejectionMessage,
                         @ModelAttribute CertFilter filter,
                         Authentication auth,
                         RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    certificationService.reject(id, rejectionMessage, auth.getName());
                    adminLogService.log(AdminAction.CERTIFICATION_REJECT, "CERTIFICATION", id,
                            rejectionMessage.isBlank() ? null : rejectionMessage);
                },
                "인증이 거절되었습니다.",
                e -> log.error("인증 거절 실패 id={}", id, e),
                "거절 처리 중 오류가 발생했습니다.",
                ra);
        return AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword());
    }
}
