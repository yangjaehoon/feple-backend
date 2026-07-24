package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.nickname.service.NicknameRestrictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/nickname-restrictions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class NicknameRestrictionAdminController {

    private final NicknameRestrictionService nicknameRestrictionService;
    private final ArtistAdminService artistService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("nicknameRestrictions", nicknameRestrictionService.findAll());
        model.addAttribute("allArtists", artistService.getAllArtistsSortedByName());
        return "admin/moderation/nickname-restrictions";
    }

    @PostMapping("/add")
    public String add(@RequestParam String word, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    nicknameRestrictionService.add(word);
                    adminLogService.log(AdminAction.NICKNAME_RESTRICTION_ADD, "NICKNAME_RESTRICTION", null, word);
                },
                "닉네임 제한 단어가 추가되었습니다.",
                e -> log.error("닉네임 제한 단어 추가 실패: word={}", word, e),
                "닉네임 제한 단어 추가 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/nickname-restrictions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    nicknameRestrictionService.delete(id);
                    adminLogService.log(AdminAction.NICKNAME_RESTRICTION_DELETE, "NICKNAME_RESTRICTION", id, null);
                },
                "삭제되었습니다.",
                e -> log.error("닉네임 제한 단어 삭제 실패: id={}", id, e),
                AdminConstants.MSG_DELETE_ERROR,
                ra);
        return "redirect:/admin/nickname-restrictions";
    }
}
