package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.support.AdminActionUtils;
import com.feple.feple_backend.admin.support.AdminConstants;
import com.feple.feple_backend.admin.support.PageParams;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.festival.suggestion.dto.FestivalSuggestionResponseDto;
import com.feple.feple_backend.festival.suggestion.service.FestivalSuggestionAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.FESTIVALS)
@Controller
@RequestMapping("/admin/festival-suggestions")
@RequiredArgsConstructor
public class FestivalSuggestionAdminController {

    private final FestivalSuggestionAdminService festivalSuggestionAdminService;
    private final FestivalAdminService festivalAdminService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String list(@ModelAttribute PageParams params, Model model) {
        Page<FestivalSuggestionResponseDto> suggestions =
                festivalSuggestionAdminService.getSuggestionsPage(params.page(), AdminConstants.LIST_PAGE_SIZE);
        model.addAttribute("suggestions", suggestions);
        // "기존 페스티벌과 연결" 모달에서 ID를 직접 입력하는 대신 이름으로 검색해 고를 수 있도록
        // 전체 페스티벌 목록을 함께 내려준다.
        model.addAttribute("allFestivals", festivalAdminService.getAllFestivalsForAdmin());
        return "admin/festival/suggestions";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam Long festivalId,
                          RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    festivalSuggestionAdminService.approve(id, festivalId);
                    adminLogService.log(AdminAction.FESTIVAL_SUGGESTION_APPROVE, "FESTIVAL_SUGGESTION", id, null);
                },
                "페스티벌 신청이 승인되었습니다.",
                e -> log.error("페스티벌 신청 승인 실패: id={}, festivalId={}", id, festivalId, e),
                "승인 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/festival-suggestions";
    }

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable Long id,
                          @RequestParam(defaultValue = "") String processNote,
                          RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    festivalSuggestionAdminService.dismiss(id, processNote.isBlank() ? null : processNote.trim());
                    adminLogService.log(AdminAction.FESTIVAL_SUGGESTION_DISMISS, "FESTIVAL_SUGGESTION", id, null);
                },
                "페스티벌 신청이 처리되었습니다.",
                e -> log.error("페스티벌 신청 처리 실패: {}", id, e),
                AdminConstants.MSG_PROCESS_ERROR,
                ra);
        return "redirect:/admin/festival-suggestions";
    }
}
