package com.feple.feple_backend.admin;

import com.feple.feple_backend.badword.service.BadWordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/admin/bad-words")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BadWordAdminController {

    private final BadWordService badWordService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("badWords", badWordService.findAll());
        return "admin/bad-words";
    }

    @PostMapping("/add")
    public String add(@RequestParam String word, RedirectAttributes ra) {
        try {
            badWordService.add(word);
            ra.addFlashAttribute("success", "금칙어가 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/bad-words";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            badWordService.delete(id);
            ra.addFlashAttribute("success", "삭제되었습니다.");
        } catch (Exception e) {
            log.error("금칙어 삭제 실패: id={}", id, e);
            ra.addFlashAttribute("error", "삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/bad-words";
    }
}
