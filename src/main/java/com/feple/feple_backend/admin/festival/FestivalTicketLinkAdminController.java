package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.support.AdminActionUtils;
import com.feple.feple_backend.admin.support.BindingResultUtils;
import com.feple.feple_backend.ticketlink.dto.TicketLinkRequestDto;
import com.feple.feple_backend.ticketlink.service.FestivalTicketLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.FESTIVALS)
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals/{festivalId}/ticket-links")
public class FestivalTicketLinkAdminController {

    private final FestivalTicketLinkService ticketLinkService;
    private final AdminLogService adminLogService;

    @PostMapping
    public String createTicketLink(@PathVariable Long festivalId,
                                    @Valid @ModelAttribute TicketLinkRequestDto dto,
                                    BindingResult bindingResult,
                                    RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("errorMessage", BindingResultUtils.firstError(bindingResult));
            return AdminFestivalRedirects.ticketLinks(festivalId);
        }
        AdminActionUtils.tryAction(
                () -> {
                    ticketLinkService.createTicketLink(festivalId, dto);
                    adminLogService.log(AdminAction.FESTIVAL_TICKET_LINK_ADD, "FESTIVAL", festivalId, dto.getUrl());
                },
                "예매 링크가 추가되었습니다.",
                e -> log.error("예매 링크 추가 실패 festivalId={}", festivalId, e),
                "예매 링크 추가에 실패했습니다.",
                ra);
        return AdminFestivalRedirects.ticketLinks(festivalId);
    }

    @PostMapping("/{ticketLinkId}/delete")
    public String deleteTicketLink(@PathVariable Long festivalId,
                                    @PathVariable Long ticketLinkId,
                                    RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    ticketLinkService.deleteTicketLink(festivalId, ticketLinkId);
                    adminLogService.log(AdminAction.FESTIVAL_TICKET_LINK_DELETE, "FESTIVAL", festivalId, "ticketLinkId=" + ticketLinkId);
                },
                "예매 링크가 삭제되었습니다.",
                e -> log.error("예매 링크 삭제 실패 festivalId={}, ticketLinkId={}", festivalId, ticketLinkId, e),
                "예매 링크 삭제에 실패했습니다.",
                ra);
        return AdminFestivalRedirects.ticketLinks(festivalId);
    }
}
