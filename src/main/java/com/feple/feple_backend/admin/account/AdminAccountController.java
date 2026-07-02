package com.feple.feple_backend.admin.account;

import com.feple.feple_backend.admin.AdminActionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Slf4j
@Controller
@RequestMapping("/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService accountService;

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
    public String create(@RequestParam String username,
                         @RequestParam String password,
                         @RequestParam(defaultValue = "") String displayName,
                         @RequestParam AdminRole role,
                         @RequestParam(required = false) Set<AdminPermission> permissions,
                         @RequestParam(required = false) MultipartFile profileImage,
                         RedirectAttributes redirectAttributes) {
        Set<AdminPermission> perms = (permissions != null) ? permissions : Set.of();
        AdminAccountCreateRequestDto req = new AdminAccountCreateRequestDto(username, password, displayName, role, perms, profileImage);
        AdminActionUtils.tryAction(
                () -> accountService.create(req),
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
                         @RequestParam(defaultValue = "") String displayName,
                         @RequestParam AdminRole role,
                         @RequestParam(required = false) Set<AdminPermission> permissions,
                         @RequestParam(defaultValue = "") String password,
                         @RequestParam(required = false) MultipartFile profileImage,
                         @RequestParam(defaultValue = "false") boolean deleteProfileImage,
                         RedirectAttributes redirectAttributes) {
        Set<AdminPermission> perms = (permissions != null) ? permissions : Set.of();
        AdminAccountUpdateRequestDto req = new AdminAccountUpdateRequestDto(displayName, role, perms, password, profileImage, deleteProfileImage);
        AdminActionUtils.tryAction(
                () -> accountService.update(id, req),
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
                () -> accountService.delete(id, auth.getName()),
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
                () -> accountService.toggleEnabled(id, auth.getName()),
                "계정 활성화 상태가 변경되었습니다.",
                e -> log.error("관리자 계정 상태 변경 중 오류 발생, id={}", id, e),
                "계정 상태 변경 중 오류가 발생했습니다.",
                redirectAttributes);
        return "redirect:/admin/accounts";
    }
}
