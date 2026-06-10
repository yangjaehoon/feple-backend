package com.feple.feple_backend.admin;

import com.feple.feple_backend.stage.service.StageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
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
        } catch (Exception e) {
            log.error("스테이지 추가 실패: festivalId={}", festivalId, e);
            ra.addFlashAttribute("errorMessage", "스테이지 추가에 실패했습니다.");
        }
        return AdminFestivalRedirects.timetable(festivalId);
    }

    @PostMapping("/{stageId}/delete")
    public String deleteStage(@PathVariable Long festivalId,
                              @PathVariable Long stageId,
                              RedirectAttributes ra) {
        try {
            stageService.deleteStage(stageId);
            ra.addFlashAttribute("successMessage", "스테이지가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("스테이지 삭제 실패: festivalId={}, stageId={}", festivalId, stageId, e);
            ra.addFlashAttribute("errorMessage", "스테이지 삭제에 실패했습니다.");
        }
        return AdminFestivalRedirects.timetable(festivalId);
    }

    @PostMapping("/{stageId}/up")
    public String moveStageUp(@PathVariable Long festivalId,
                              @PathVariable Long stageId,
                              RedirectAttributes ra) {
        try {
            stageService.moveUp(festivalId, stageId);
        } catch (Exception e) {
            log.error("스테이지 순서 변경 실패: festivalId={}, stageId={}", festivalId, stageId, e);
            ra.addFlashAttribute("errorMessage", "순서 변경에 실패했습니다.");
        }
        return AdminFestivalRedirects.timetable(festivalId);
    }

    @PostMapping("/{stageId}/down")
    public String moveStageDown(@PathVariable Long festivalId,
                                @PathVariable Long stageId,
                                RedirectAttributes ra) {
        try {
            stageService.moveDown(festivalId, stageId);
        } catch (Exception e) {
            log.error("스테이지 순서 변경 실패: festivalId={}, stageId={}", festivalId, stageId, e);
            ra.addFlashAttribute("errorMessage", "순서 변경에 실패했습니다.");
        }
        return AdminFestivalRedirects.timetable(festivalId);
    }
}
