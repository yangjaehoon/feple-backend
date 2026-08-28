package com.feple.feple_backend.admin.certification;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.CERTIFICATIONS)
@Controller
@RequestMapping("/admin/certifications")
@RequiredArgsConstructor
public class CertificationAdminController {

    private final FestivalCertificationAdminService certificationService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String list(@ModelAttribute CertificationFilter filter, Model model) {
        CertificationStatus status = parseStatus(filter.status());
        Page<FestivalCertification> certPage = !filter.keyword().isBlank()
                ? certificationService.searchByKeyword(filter.keyword(), status, filter.page())
                : certificationService.getByStatus(status, filter.page());

        model.addAttribute("certifications", certPage);
        model.addAttribute("status", filter.status());
        model.addAttribute("keyword", filter.keyword());
        model.addAttribute("page", filter.page());
        model.addAttribute("pendingCount", certificationService.getPendingCount());
        model.addAttribute("totalCount", certificationService.getTotalCount());
        return "admin/certification/list";
    }

    private static CertificationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return CertificationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         @ModelAttribute CertificationFilter filter,
                         Model model, RedirectAttributes ra) {
        return AdminActionUtils.tryRender(
                () -> {
                    FestivalCertification cert = certificationService.getById(id);
                    model.addAttribute("cert", cert);
                    model.addAttribute("photoUrl", certificationService.buildPhotoUrl(cert.getPhotoKey()));
                    model.addAttribute("returnStatus", filter.status());
                    model.addAttribute("returnPage", filter.page());
                    model.addAttribute("returnKeyword", filter.keyword());
                    model.addAttribute("nextCertId", certificationService.findNextPendingId(id).orElse(null));
                    model.addAttribute("returnUrl", AdminActionUtils.listUrl("/admin/certifications",
                            filter.status(), filter.page(), filter.keyword()));
                },
                "admin/certification/detail",
                e -> log.error("인증 상세 조회 실패 id={}", id, e),
                "인증 정보를 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/certifications",
                ra);
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @ModelAttribute CertificationFilter filter,
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
                         @ModelAttribute CertificationFilter filter,
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
                              @ModelAttribute CertificationFilter filter,
                              Authentication auth,
                              RedirectAttributes ra) {
        String invalidSelection = AdminActionUtils.requireValidSelection(
                ids, AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword()), ra);
        if (invalidSelection != null) return invalidSelection;
        AdminActionUtils.tryAction(
                () -> {
                    certificationService.bulkApprove(ids, auth.getName());
                    adminLogService.log(AdminAction.CERTIFICATION_BULK_APPROVE, "CERTIFICATION", null,
                            "승인 " + AdminActionUtils.describeIds(ids));
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
                             @ModelAttribute CertificationFilter filter,
                             Authentication auth,
                             RedirectAttributes ra) {
        String invalidSelection = AdminActionUtils.requireValidSelection(
                ids, AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword()), ra);
        if (invalidSelection != null) return invalidSelection;
        AdminActionUtils.tryAction(
                () -> {
                    certificationService.bulkReject(ids, rejectionMessage, auth.getName());
                    adminLogService.log(AdminAction.CERTIFICATION_BULK_REJECT, "CERTIFICATION", null,
                            "거절 " + AdminActionUtils.describeIds(ids)
                                    + (rejectionMessage.isBlank() ? "" : " / " + rejectionMessage));
                },
                ids.size() + "건이 거절되었습니다.",
                e -> log.error("인증 일괄 거절 실패 ids={}", ids, e),
                "일괄 거절 처리 중 오류가 발생했습니다.",
                ra);
        return AdminActionUtils.listRedirect("/admin/certifications", filter.status(), filter.page(), filter.keyword());
    }

    private String triageRedirect(Long nextCertId, CertificationFilter filter) {
        return AdminActionUtils.listRedirect("/admin/certifications/" + nextCertId,
                filter.status(), filter.page(), filter.keyword());
    }
}
