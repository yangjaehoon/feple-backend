package com.feple.feple_backend.admin;

import com.feple.feple_backend.stage.service.StageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals/{festivalId}/stages")
public class FestivalStageAdminController {

    private final StageService stageService;

    @PostMapping
    public String createStage(@PathVariable Long festivalId,
                              @RequestParam String name,
                              RedirectAttributes ra) {
        try {
            stageService.createStage(festivalId, name);
            ra.addFlashAttribute("successMessage", "스테이지가 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/festivals/" + festivalId + "#timetable";
    }

    @PostMapping("/{stageId}/delete")
    public String deleteStage(@PathVariable Long festivalId,
                              @PathVariable Long stageId,
                              RedirectAttributes ra) {
        stageService.deleteStage(stageId);
        ra.addFlashAttribute("successMessage", "스테이지가 삭제되었습니다.");
        return "redirect:/admin/festivals/" + festivalId + "#timetable";
    }

    @PostMapping("/{stageId}/up")
    public String moveStageUp(@PathVariable Long festivalId, @PathVariable Long stageId) {
        stageService.moveUp(festivalId, stageId);
        return "redirect:/admin/festivals/" + festivalId + "#timetable";
    }

    @PostMapping("/{stageId}/down")
    public String moveStageDown(@PathVariable Long festivalId, @PathVariable Long stageId) {
        stageService.moveDown(festivalId, stageId);
        return "redirect:/admin/festivals/" + festivalId + "#timetable";
    }
}
