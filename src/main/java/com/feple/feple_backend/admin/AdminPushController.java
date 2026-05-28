package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Controller
@RequestMapping("/admin/push")
@RequiredArgsConstructor
public class AdminPushController {

    private final AdminPushService adminPushService;
    private final AdminLogService adminLogService;
    private final UserAdminService userAdminService;

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("deviceCount", adminPushService.getRegisteredDeviceCount());
        model.addAttribute("history", adminPushService.getBroadcastHistory());
        return "admin/admin-push";
    }

    @PostMapping
    public String send(@RequestParam String title,
                       @RequestParam String body,
                       RedirectAttributes ra) {
        try {
            if (title.isBlank() || body.isBlank()) {
                ra.addFlashAttribute("errorMessage", "제목과 내용을 모두 입력해주세요.");
                return "redirect:/admin/push";
            }
            adminPushService.sendToAll(title.strip(), body.strip());
            adminLogService.log("PUSH_BROADCAST", null, null, title.strip());
            ra.addFlashAttribute("successMessage", "푸시 알림이 발송되었습니다.");
        } catch (Exception e) {
            log.error("푸시 발송 오류", e);
            ra.addFlashAttribute("errorMessage", "발송 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/push";
    }

    @GetMapping("/search-user")
    @ResponseBody
    public ResponseEntity<?> searchUser(@RequestParam String nickname) {
        try {
            var user = userAdminService.findByNickname(nickname);
            return ResponseEntity.ok(Map.of("id", user.getId(), "nickname", user.getNickname()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test")
    public String sendTest(@RequestParam String title,
                           @RequestParam String body,
                           @RequestParam Long targetUserId,
                           RedirectAttributes ra) {
        try {
            if (title.isBlank() || body.isBlank()) {
                ra.addFlashAttribute("errorMessage", "제목과 내용을 모두 입력해주세요.");
                return "redirect:/admin/push";
            }
            adminPushService.sendTest(targetUserId, title.strip(), body.strip());
            adminLogService.log("PUSH_TEST", "USER", targetUserId, title.strip());
            ra.addFlashAttribute("successMessage", "테스트 발송 완료 (userId=" + targetUserId + ")");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("테스트 발송 오류", e);
            ra.addFlashAttribute("errorMessage", "발송 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/push";
    }
}
