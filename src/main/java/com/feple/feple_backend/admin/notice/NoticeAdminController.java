package com.feple.feple_backend.admin.notice;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.BindingResultUtils;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.notice.dto.NoticeRequestDto;
import com.feple.feple_backend.notice.dto.NoticeResponseDto;
import com.feple.feple_backend.notice.service.NoticeAdminService;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.NOTICES)
@Controller
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
public class NoticeAdminController {

    private final NoticeAdminService noticeAdminService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<NoticeResponseDto> notices = noticeAdminService.getAdminNotices(
                PageRequest.of(page, AdminConstants.LIST_PAGE_SIZE));
        model.addAttribute("notices", notices);
        return "admin/notice/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("notice", new NoticeRequestDto());
        return "admin/notice/create";
    }

    @PostMapping("/new")
    public String createNotice(@Valid @ModelAttribute("notice") NoticeRequestDto dto,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
            return "admin/notice/create";
        }
        Long noticeId = noticeAdminService.createNotice(dto);
        adminLogService.log(AdminAction.NOTICE_CREATE, "NOTICE", noticeId, dto.getTitle());
        ra.addFlashAttribute("successMessage", "공지사항이 등록되었습니다.");
        return "redirect:/admin/notices";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               @RequestParam(defaultValue = "0") int page,
                               Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("noticeId", id);
            model.addAttribute("notice", noticeAdminService.getNoticeForEdit(id));
            model.addAttribute("page", page);
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/notices";
        }
        return "admin/notice/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateNotice(@PathVariable Long id,
                               @Valid @ModelAttribute("notice") NoticeRequestDto dto,
                               BindingResult bindingResult,
                               @RequestParam(defaultValue = "0") int page,
                               Model model,
                               RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("noticeId", id);
            model.addAttribute("page", page);
            model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
            return "admin/notice/edit";
        }
        AdminActionUtils.tryAction(
                () -> {
                    noticeAdminService.updateNotice(id, dto);
                    adminLogService.log(AdminAction.NOTICE_UPDATE, "NOTICE", id, dto.getTitle());
                },
                "공지사항이 수정되었습니다.",
                e -> log.error("공지사항 수정 실패 id={}", id, e),
                "수정 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/notices?page=" + page;
    }

    @PostMapping("/{id}/pin")
    public String togglePin(@PathVariable Long id,
                            @RequestParam(defaultValue = "0") int page,
                            RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    noticeAdminService.togglePin(id);
                    adminLogService.log(AdminAction.NOTICE_PIN_TOGGLE, "NOTICE", id, null);
                },
                "고정 상태가 변경되었습니다.",
                e -> log.error("공지사항 고정 토글 실패 id={}", id, e),
                "처리 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/notices?page=" + page;
    }

    @PostMapping("/{id}/delete")
    public String deleteNotice(@PathVariable Long id,
                               @RequestParam(defaultValue = "0") int page,
                               RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    noticeAdminService.deleteNotice(id);
                    adminLogService.log(AdminAction.NOTICE_DELETE, "NOTICE", id, null);
                },
                "공지사항이 삭제되었습니다.",
                e -> log.error("공지사항 삭제 실패 id={}", id, e),
                AdminConstants.MSG_DELETE_ERROR,
                ra);
        return "redirect:/admin/notices?page=" + page;
    }
}
