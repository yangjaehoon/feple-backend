package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.checklist.FestivalChecklistService;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals")
public class FestivalChecklistAdminController {

    private final FestivalChecklistService festivalChecklistService;
    private final AdminLogService adminLogService;

    @PostMapping("/{id}/checklist")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleChecklist(@PathVariable Long id,
                                                               @RequestParam String field) {
        try {
            boolean newValue = festivalChecklistService.toggle(id, field);
            adminLogService.log(AdminAction.FESTIVAL_CHECKLIST_TOGGLE, "FESTIVAL", id, field + "=" + newValue);
            return ResponseEntity.ok(Map.of("checked", newValue));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static final int MEMO_MAX_LENGTH = 1000;

    @PostMapping("/{id}/checklist/memo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveMemo(@PathVariable Long id,
                                                        @RequestParam String memo) {
        if (memo.length() > MEMO_MAX_LENGTH) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "메모는 " + MEMO_MAX_LENGTH + "자 이하여야 합니다."));
        }
        festivalChecklistService.saveMemo(id, memo);
        adminLogService.log(AdminAction.FESTIVAL_CHECKLIST_MEMO, "FESTIVAL", id, null);
        return ResponseEntity.ok(Map.of("saved", true));
    }
}
