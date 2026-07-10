package com.feple.feple_backend.admin.certification;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/certifications")
@RequiredArgsConstructor
public class CertificationAdminController {

    private final FestivalCertificationAdminService certificationService;
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
    public String detail(@PathVariable Long id,
                         @RequestParam(required = false, defaultValue = "") String status,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(required = false, defaultValue = "") String keyword,
                         Model model, RedirectAttributes ra) {
        return AdminActionUtils.tryRender(
                () -> {
                    FestivalCertification cert = certificationService.getById(id);
                    model.addAttribute("cert", cert);
                    model.addAttribute("photoUrl", certificationService.buildPhotoUrl(cert.getPhotoKey()));
                    model.addAttribute("returnStatus", status);
                    model.addAttribute("returnPage", page);
                    model.addAttribute("returnKeyword", keyword);
                    model.addAttribute("nextCertId", certificationService.findNextPendingId(id).orElse(null));
                    UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/certifications")
                            .queryParam("status", status)
                            .queryParam("page", page);
                    if (!keyword.isBlank()) builder.queryParam("keyword", keyword);
                    model.addAttribute("returnUrl", builder.build().toUriString());
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
                          @RequestParam(required = false) Long nextCertId,
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
        if (nextCertId != null) return triageRedirect(nextCertId, filter);
        return AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword());
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String rejectionMessage,
                         @ModelAttribute CertFilter filter,
                         @RequestParam(required = false) Long nextCertId,
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
        if (nextCertId != null) return triageRedirect(nextCertId, filter);
        return AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword());
    }

    @PostMapping("/bulk-approve")
    public String bulkApprove(@RequestParam(required = false) List<Long> ids,
                              @ModelAttribute CertFilter filter,
                              Authentication auth,
                              RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "선택된 항목이 없습니다.");
            return AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword());
        }
        AdminActionUtils.tryAction(
                () -> {
                    certificationService.bulkApprove(ids, auth.getName());
                    adminLogService.log(AdminAction.CERTIFICATION_BULK_APPROVE, "CERTIFICATION", null,
                            ids.size() + "건 일괄 승인");
                },
                ids.size() + "건이 승인되었습니다.",
                e -> log.error("인증 일괄 승인 실패 ids={}", ids, e),
                "일괄 승인 처리 중 오류가 발생했습니다.",
                ra);
        return AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword());
    }

    @PostMapping("/bulk-reject")
    public String bulkReject(@RequestParam(required = false) List<Long> ids,
                             @RequestParam(defaultValue = "") String rejectionMessage,
                             @ModelAttribute CertFilter filter,
                             Authentication auth,
                             RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "선택된 항목이 없습니다.");
            return AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword());
        }
        AdminActionUtils.tryAction(
                () -> {
                    certificationService.bulkReject(ids, rejectionMessage, auth.getName());
                    adminLogService.log(AdminAction.CERTIFICATION_BULK_REJECT, "CERTIFICATION", null,
                            ids.size() + "건 일괄 거절");
                },
                ids.size() + "건이 거절되었습니다.",
                e -> log.error("인증 일괄 거절 실패 ids={}", ids, e),
                "일괄 거절 처리 중 오류가 발생했습니다.",
                ra);
        return AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword());
    }

    private String triageRedirect(Long nextCertId, CertFilter filter) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/certifications/" + nextCertId)
                .queryParam("status", filter.status())
                .queryParam("page", filter.page());
        if (!filter.keyword().isBlank()) builder.queryParam("keyword", filter.keyword());
        return "redirect:" + builder.build().toUriString();
    }
}
