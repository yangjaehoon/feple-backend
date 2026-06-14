package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.badword.service.BadWordService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.nickname.service.NicknameRestrictionService;
import com.feple.feple_backend.post.service.PostAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/admin/bad-words")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BadWordAdminController {

    private final BadWordService badWordService;
    private final PostAdminService postAdminService;
    private final CommentService commentService;
    private final AdminLogService adminLogService;
    private final NicknameRestrictionService nicknameRestrictionService;
    private final ArtistService artistService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("badWords", badWordService.findAll());
        model.addAttribute("nicknameRestrictions", nicknameRestrictionService.findAll());
        model.addAttribute("artistNames", artistService.getAllArtistsSortedByName());
        return "admin/moderation/bad-words";
    }

    @PostMapping("/add")
    public String add(@RequestParam String word, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    badWordService.add(word);
                    adminLogService.log(AdminAction.BAD_WORD_ADD, "BAD_WORD", null, word);
                },
                "금칙어가 추가되었습니다.",
                e -> log.error("금칙어 추가 실패: word={}", word, e),
                "금칙어 추가 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/bad-words";
    }

    @GetMapping("/scan")
    @ResponseBody
    public ResponseEntity<?> scan(@RequestParam String word) {
        if (word.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "단어를 입력해주세요."));
        }
        long postCount    = postAdminService.countPostsContaining(word.strip());
        long commentCount = commentService.countCommentsContaining(word.strip());
        return ResponseEntity.ok(Map.of("postCount", postCount, "commentCount", commentCount));
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    badWordService.delete(id);
                    adminLogService.log(AdminAction.BAD_WORD_DELETE, "BAD_WORD", id, null);
                },
                "삭제되었습니다.",
                e -> log.error("금칙어 삭제 실패: id={}", id, e),
                "삭제 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/bad-words";
    }

    @PostMapping("/nickname-restrictions/add")
    public String addNicknameRestriction(@RequestParam String word, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    nicknameRestrictionService.add(word);
                    adminLogService.log(AdminAction.NICKNAME_RESTRICTION_ADD, "NICKNAME_RESTRICTION", null, word);
                },
                "닉네임 제한 단어가 추가되었습니다.",
                e -> log.error("닉네임 제한 단어 추가 실패: word={}", word, e),
                "닉네임 제한 단어 추가 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/bad-words#nickname-restrictions";
    }

    @PostMapping("/nickname-restrictions/{id}/delete")
    public String deleteNicknameRestriction(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    nicknameRestrictionService.delete(id);
                    adminLogService.log(AdminAction.NICKNAME_RESTRICTION_DELETE, "NICKNAME_RESTRICTION", id, null);
                },
                "삭제되었습니다.",
                e -> log.error("닉네임 제한 단어 삭제 실패: id={}", id, e),
                "삭제 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/bad-words#nickname-restrictions";
    }
}
