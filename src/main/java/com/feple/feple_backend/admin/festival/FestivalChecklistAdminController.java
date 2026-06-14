package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.FestivalChecklistService;
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

    @PostMapping("/{id}/checklist")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleChecklist(@PathVariable Long id,
                                                               @RequestParam String field) {
        try {
            festivalChecklistService.toggle(id, field);
            boolean newValue = festivalChecklistService.isChecked(id, field);
            return ResponseEntity.ok(Map.of("checked", newValue));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/checklist/memo")
    @ResponseBody
    public ResponseEntity<Void> saveMemo(@PathVariable Long id,
                                         @RequestParam String memo) {
        festivalChecklistService.saveMemo(id, memo);
        return ResponseEntity.ok().build();
    }
}
