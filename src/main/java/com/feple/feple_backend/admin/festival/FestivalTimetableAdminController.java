package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.BindingResultUtils;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequest;
import com.feple.feple_backend.timetable.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals/{festivalId}/timetable")
public class FestivalTimetableAdminController {

    private final TimetableService timetableService;

    @PostMapping
    public String createTimetableEntry(@PathVariable Long festivalId,
                                       @Valid @ModelAttribute TimetableEntryRequest req,
                                       BindingResult bindingResult,
                                       RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("errorMessage", BindingResultUtils.firstError(bindingResult));
            return AdminFestivalRedirects.timetable(festivalId);
        }
        AdminActionUtils.tryAction(
                () -> timetableService.createEntry(festivalId, req),
                "타임테이블 항목이 추가되었습니다.",
                e -> log.error("타임테이블 항목 추가 실패: festivalId={}", festivalId, e),
                "항목 추가 중 오류가 발생했습니다.",
                ra);
        return AdminFestivalRedirects.timetable(festivalId);
    }

    @PostMapping("/{entryId}/delete")
    public String deleteTimetableEntry(@PathVariable Long festivalId,
                                       @PathVariable Long entryId,
                                       RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> timetableService.deleteEntry(festivalId, entryId),
                "항목이 삭제되었습니다.",
                e -> log.error("타임테이블 항목 삭제 실패: festivalId={}, entryId={}", festivalId, entryId, e),
                "항목 삭제에 실패했습니다.",
                ra);
        return AdminFestivalRedirects.timetable(festivalId);
    }
}
