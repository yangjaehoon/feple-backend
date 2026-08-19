package com.feple.feple_backend.admin.notice;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.BindingResultUtils;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.push.AdminPushService;
import com.feple.feple_backend.notice.dto.NoticeRequestDto;
import com.feple.feple_backend.notice.dto.NoticeSummaryDto;
import com.feple.feple_backend.notice.service.NoticeAdminService;
import jakarta.validation.Valid;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AdminPushService adminPushService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<NoticeSummaryDto> notices = noticeAdminService.getAdminNotices(
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
        AdminActionUtils.tryActionWithResult(
                () -> {
                    Long noticeId = noticeAdminService.createNotice(dto);
                    adminLogService.log(AdminAction.NOTICE_CREATE, "NOTICE", noticeId, dto.getTitle());
                    return pushFailed(noticeId, dto);
                },
                pushFailed -> pushFailed
                        ? "공지사항이 등록되었습니다. (알림 발송에는 실패했습니다)"
                        : "공지사항이 등록되었습니다.",
                e -> log.error("공지사항 등록 실패 title={}", dto.getTitle(), e),
                "등록 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/notices";
    }

    // 전체 발송(AdminPushService.sendToAll)은 기존에 SUPER_ADMIN 전용 화면(/admin/push)에서만
    // 쓸 수 있던 기능이라, NOTICES 권한만 있는 관리자가 공지 등록으로 우회해 전체 푸시를 보내지
    // 못하도록 여기서도 SUPER_ADMIN 여부를 다시 검사한다(화면에서 체크박스를 숨겨도 폼 위조로
    // 우회될 수 있음). 발송에 실패해도 공지 등록 자체는 이미 커밋된 별도 트랜잭션이라 실패로
    // 처리하지 않고, 실패 여부만 성공 메시지에 반영한다.
    private boolean pushFailed(Long noticeId, NoticeRequestDto dto) {
        if (!dto.isSendNotification()) return false;
        if (!isSuperAdmin()) {
            log.warn("[NoticeAdmin] SUPER_ADMIN이 아닌 관리자가 알림 발송을 요청해 무시함 noticeId={}", noticeId);
            return false;
        }
        try {
            adminPushService.sendToAll(
                    truncate(dto.getTitle(), AdminConstants.PUSH_TITLE_MAX_LENGTH),
                    truncate(dto.getContent(), AdminConstants.PUSH_BODY_MAX_LENGTH));
            adminLogService.log(AdminAction.NOTICE_PUSH, "NOTICE", noticeId, dto.getTitle());
            return false;
        } catch (Exception e) {
            log.error("공지사항 알림 발송 실패 noticeId={}", noticeId, e);
            return true;
        }
    }

    // 잘라낼 길이가 넉넉할 때(최대 길이의 절반 이전)만 마지막 공백 기준으로 맞춘다 —
    // 공백이 너무 앞쪽에만 있으면(또는 아예 없으면) 단어 경계를 맞추려다 내용을 과도하게
    // 잘라내는 역효과가 나서, 그럴 땐 그냥 글자 수 기준으로 자른다.
    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        String cut = text.substring(0, maxLength - 1);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > maxLength / 2) {
            cut = cut.substring(0, lastSpace);
        }
        return cut + "…";
    }

    private boolean isSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
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
        return noticesRedirect(page);
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
        return noticesRedirect(page);
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
        return noticesRedirect(page);
    }

    private static String noticesRedirect(int page) {
        return "redirect:/admin/notices?page=" + page;
    }
}
