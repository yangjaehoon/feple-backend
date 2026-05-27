package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/artist-suggestions")
@RequiredArgsConstructor
public class ArtistSuggestionAdminController {

    private final ArtistSuggestionAdminService artistSuggestionAdminService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("suggestions", artistSuggestionAdminService.getPendingSuggestions());
        return "admin/artist-suggestions";
    }

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable Long id, RedirectAttributes ra) {
        try {
            artistSuggestionAdminService.dismiss(id);
            ra.addFlashAttribute("successMessage", "아티스트 신청이 처리되었습니다.");
        } catch (Exception e) {
            log.error("아티스트 신청 처리 실패: {}", id, e);
            ra.addFlashAttribute("errorMessage", "처리 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/artist-suggestions";
    }
}
