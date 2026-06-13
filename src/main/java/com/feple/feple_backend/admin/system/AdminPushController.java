package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminPushService;
import com.feple.feple_backend.admin.log.AdminAction;
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

    private static final String PUSH_REDIRECT   = "redirect:/admin/push";
    private static final String SEND_ERROR_MSG  = "발송 중 오류가 발생했습니다.";

    private final AdminPushService adminPushService;
    private final AdminLogService adminLogService;
    private final UserAdminService userAdminService;

    @GetMapping
    public String showForm(Model model) {
        PushFormData data = adminPushService.getFormData();
        model.addAttribute("deviceCount", data.deviceCount());
        model.addAttribute("history",     data.history());
        model.addAttribute("artists",     data.artists());
        model.addAttribute("festivals",   data.festivals());
        return "admin/system/push";
    }

    @PostMapping
    public String send(@RequestParam String title,
                       @RequestParam String body,
                       RedirectAttributes ra) {
        if (!validatePushInput(title, body, ra)) return PUSH_REDIRECT;
        AdminActionUtils.tryAction(
                () -> {
                    adminPushService.sendToAll(title.strip(), body.strip());
                    adminLogService.log(AdminAction.PUSH_BROADCAST, null, null, title.strip());
                },
                "푸시 알림이 발송되었습니다.",
                e -> log.error("푸시 발송 오류 [전체 발송]", e),
                SEND_ERROR_MSG,
                ra);
        return PUSH_REDIRECT;
    }

    @GetMapping("/search-user")
    @ResponseBody
    public ResponseEntity<?> searchUser(@RequestParam String nickname) {
        try {
            var user = userAdminService.findByNickname(nickname);
            return ResponseEntity.ok(Map.of("id", user.getId(), "nickname", user.getNickname()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", "해당 닉네임의 사용자를 찾을 수 없습니다."));
        }
    }

    @PostMapping("/artist-followers")
    public String sendToArtistFollowers(@RequestParam String title,
                                        @RequestParam String body,
                                        @RequestParam Long artistId,
                                        RedirectAttributes ra) {
        if (!validatePushInput(title, body, ra)) return PUSH_REDIRECT;
        AdminActionUtils.tryAction(
                () -> {
                    adminPushService.sendToArtistFollowers(artistId, title.strip(), body.strip());
                    adminLogService.log(AdminAction.PUSH_ARTIST_FOLLOWERS, "ARTIST", artistId, title.strip());
                },
                "아티스트 팔로워에게 발송되었습니다.",
                e -> log.error("푸시 발송 오류 [아티스트 팔로워 발송]", e),
                SEND_ERROR_MSG,
                ra);
        return PUSH_REDIRECT;
    }

    @PostMapping("/festival-certified")
    public String sendToFestivalCertified(@RequestParam String title,
                                          @RequestParam String body,
                                          @RequestParam Long festivalId,
                                          RedirectAttributes ra) {
        if (!validatePushInput(title, body, ra)) return PUSH_REDIRECT;
        AdminActionUtils.tryAction(
                () -> {
                    adminPushService.sendToFestivalCertified(festivalId, title.strip(), body.strip());
                    adminLogService.log(AdminAction.PUSH_FESTIVAL_CERTIFIED, "FESTIVAL", festivalId, title.strip());
                },
                "페스티벌 인증 참여자에게 발송되었습니다.",
                e -> log.error("푸시 발송 오류 [페스티벌 인증자 발송]", e),
                SEND_ERROR_MSG,
                ra);
        return PUSH_REDIRECT;
    }

    @PostMapping("/test")
    public String sendTest(@RequestParam String title,
                           @RequestParam String body,
                           @RequestParam Long targetUserId,
                           RedirectAttributes ra) {
        if (!validatePushInput(title, body, ra)) return PUSH_REDIRECT;
        AdminActionUtils.tryAction(
                () -> {
                    adminPushService.sendTest(targetUserId, title.strip(), body.strip());
                    adminLogService.log(AdminAction.PUSH_TEST, "USER", targetUserId, title.strip());
                },
                "테스트 발송 완료 (userId=" + targetUserId + ")",
                e -> log.error("푸시 발송 오류 [테스트 발송]", e),
                SEND_ERROR_MSG,
                ra);
        return PUSH_REDIRECT;
    }

    private boolean validatePushInput(String title, String body, RedirectAttributes ra) {
        if (title.isBlank() || body.isBlank()) {
            ra.addFlashAttribute("errorMessage", "제목과 내용을 모두 입력해주세요.");
            return false;
        }
        if (title.length() > 100) {
            ra.addFlashAttribute("errorMessage", "푸시 제목은 100자 이하여야 합니다.");
            return false;
        }
        if (body.length() > 500) {
            ra.addFlashAttribute("errorMessage", "푸시 내용은 500자 이하여야 합니다.");
            return false;
        }
        return true;
    }
}
