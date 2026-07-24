package com.feple.feple_backend.admin.account;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/accounts")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService accountService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("accounts", accountService.findAll());
        model.addAttribute("allPermissions", AdminPermission.values());
        model.addAttribute("allRoles", AdminRole.values());
        return "admin/account/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("allPermissions", AdminPermission.values());
        model.addAttribute("allRoles", AdminRole.values());
        return "admin/account/form";
    }

    @PostMapping(consumes = "multipart/form-data")
    public String create(@ModelAttribute AdminAccountCreateRequestDto req,
                         RedirectAttributes redirectAttributes) {
        AdminActionUtils.tryAction(
                () -> {
                    AdminAccount account = accountService.create(req);
                    adminLogService.log(AdminAction.ADMIN_ACCOUNT_CREATE, "ADMIN_ACCOUNT", account.getId(), account.getUsername());
                },
                "관리자 계정이 생성되었습니다.",
                e -> log.error("관리자 계정 생성 중 오류 발생", e),
                "계정 생성 중 오류가 발생했습니다.",
                redirectAttributes);
        return "redirect:/admin/accounts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("account", accountService.findById(id));
        model.addAttribute("allPermissions", AdminPermission.values());
        model.addAttribute("allRoles", AdminRole.values());
        return "admin/account/form";
    }

    @PostMapping(value = "/{id}/update", consumes = "multipart/form-data")
    public String update(@PathVariable Long id,
                         @ModelAttribute AdminAccountUpdateRequestDto req,
                         RedirectAttributes redirectAttributes) {
        AdminActionUtils.tryAction(
                () -> {
                    accountService.update(id, req);
                    adminLogService.log(AdminAction.ADMIN_ACCOUNT_UPDATE, "ADMIN_ACCOUNT", id, req.displayName());
                },
                "관리자 계정이 수정되었습니다.",
                e -> log.error("관리자 계정 수정 중 오류 발생, id={}", id, e),
                "계정 수정 중 오류가 발생했습니다.",
                redirectAttributes);
        return "redirect:/admin/accounts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         Authentication auth,
                         RedirectAttributes redirectAttributes) {
        AdminActionUtils.tryAction(
                () -> {
                    AdminAccount account = accountService.delete(id, auth.getName());
                    adminLogService.log(AdminAction.ADMIN_ACCOUNT_DELETE, "ADMIN_ACCOUNT", id, account.getUsername());
                },
                "관리자 계정이 삭제되었습니다.",
                e -> log.error("관리자 계정 삭제 중 오류 발생, id={}", id, e),
                "계정 삭제 중 오류가 발생했습니다.",
                redirectAttributes);
        return "redirect:/admin/accounts";
    }

    @PostMapping("/{id}/toggle-enabled")
    public String toggleEnabled(@PathVariable Long id,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        AdminActionUtils.tryAction(
                () -> {
                    AdminAccount account = accountService.toggleEnabled(id, auth.getName());
                    String detail = account.getUsername() + " → " + (account.isEnabled() ? "활성" : "비활성");
                    adminLogService.log(AdminAction.ADMIN_ACCOUNT_TOGGLE, "ADMIN_ACCOUNT", id, detail);
                },
                "계정 활성화 상태가 변경되었습니다.",
                e -> log.error("관리자 계정 상태 변경 중 오류 발생, id={}", id, e),
                "계정 상태 변경 중 오류가 발생했습니다.",
                redirectAttributes);
        return "redirect:/admin/accounts";
    }
}
