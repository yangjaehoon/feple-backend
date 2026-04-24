package com.feple.feple_backend.admin;

import com.feple.feple_backend.timetable.dto.TimetableEntryRequest;
import com.feple.feple_backend.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals/{festivalId}/timetable")
public class FestivalTimetableAdminController {

    private final TimetableService timetableService;

    @PostMapping
    public String createTimetableEntry(@PathVariable Long festivalId,
                                       @ModelAttribute TimetableEntryRequest req,
                                       RedirectAttributes ra) {
        try {
            timetableService.createEntry(festivalId, req);
            ra.addFlashAttribute("timetableSuccess", "타임테이블 항목이 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/festivals/" + festivalId + "#timetable";
    }

    @PostMapping("/{entryId}/delete")
    public String deleteTimetableEntry(@PathVariable Long festivalId,
                                       @PathVariable Long entryId,
                                       RedirectAttributes ra) {
        timetableService.deleteEntry(entryId);
        ra.addFlashAttribute("successMessage", "항목이 삭제되었습니다.");
        return "redirect:/admin/festivals/" + festivalId + "#timetable";
    }
}
