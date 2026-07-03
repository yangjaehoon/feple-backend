package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
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
    private final AdminLogService adminLogService;

    @PostMapping
    public String createStage(@PathVariable Long festivalId,
                              @RequestParam String name,
                              RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    stageService.createStage(festivalId, name);
                    adminLogService.log(AdminAction.FESTIVAL_STAGE_ADD, "FESTIVAL", festivalId, name);
                },
                "스테이지가 추가되었습니다.",
                e -> log.error("스테이지 추가 실패: festivalId={}", festivalId, e),
                "스테이지 추가에 실패했습니다.",
                ra);
        return AdminFestivalRedirects.timetable(festivalId);
    }

    @PostMapping("/{stageId}/delete")
    public String deleteStage(@PathVariable Long festivalId,
                              @PathVariable Long stageId,
                              RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    stageService.deleteStage(stageId);
                    adminLogService.log(AdminAction.FESTIVAL_STAGE_DELETE, "FESTIVAL", festivalId, "stageId=" + stageId);
                },
                "스테이지가 삭제되었습니다.",
                e -> log.error("스테이지 삭제 실패: festivalId={}, stageId={}", festivalId, stageId, e),
                "스테이지 삭제에 실패했습니다.",
                ra);
        return AdminFestivalRedirects.timetable(festivalId);
    }

    @PostMapping("/{stageId}/up")
    public String moveStageUp(@PathVariable Long festivalId,
                              @PathVariable Long stageId,
                              RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> stageService.moveUp(festivalId, stageId),
                null,
                e -> log.error("스테이지 순서 변경 실패: festivalId={}, stageId={}", festivalId, stageId, e),
                "순서 변경에 실패했습니다.",
                ra);
        return AdminFestivalRedirects.timetable(festivalId);
    }

    @PostMapping("/{stageId}/down")
    public String moveStageDown(@PathVariable Long festivalId,
                                @PathVariable Long stageId,
                                RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> stageService.moveDown(festivalId, stageId),
                null,
                e -> log.error("스테이지 순서 변경 실패: festivalId={}, stageId={}", festivalId, stageId, e),
                "순서 변경에 실패했습니다.",
                ra);
        return AdminFestivalRedirects.timetable(festivalId);
    }
}
