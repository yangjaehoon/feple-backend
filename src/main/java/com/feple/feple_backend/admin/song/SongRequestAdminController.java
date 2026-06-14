package com.feple.feple_backend.admin.song;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.service.SongRequestAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/song-requests")
@RequiredArgsConstructor
public class SongRequestAdminController {

    private final SongRequestAdminService songRequestAdminService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String list(@RequestParam(defaultValue = AdminConstants.STATUS_PENDING) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String keyword,
                       Model model) {
        Page<SongRequestResponseDto> requests = songRequestAdminService.getRequestsPage(page, AdminConstants.LIST_PAGE_SIZE, status, keyword);
        model.addAttribute("requests", requests);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pendingCount", songRequestAdminService.getPendingCount());
        return "admin/song-request/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String youtubeUrl,
                          @RequestParam(defaultValue = AdminConstants.STATUS_PENDING) String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "") String keyword,
                          RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    boolean songSaved = songRequestAdminService.approve(id, youtubeUrl);
                    adminLogService.log(AdminAction.SONG_REQUEST_APPROVE, "SONG_REQUEST", id, null);
                    ra.addFlashAttribute("successMessage", approveMessage(songSaved, youtubeUrl));
                },
                null,
                e -> log.error("노래 요청 승인 실패 id={}", id, e),
                "승인 처리 중 오류가 발생했습니다.",
                ra);
        return AdminActionUtils.listRedirect("/admin/song-requests", status, page, keyword);
    }

    private static String approveMessage(boolean songSaved, String youtubeUrl) {
        if (songSaved) return "노래 요청이 승인되었습니다. 곡이 등록되었습니다.";
        if (youtubeUrl != null && !youtubeUrl.isBlank()) return "승인되었습니다. (YouTube 영상 정보를 가져오지 못해 곡은 등록되지 않았습니다.)";
        return "노래 요청이 승인되었습니다.";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         @RequestParam(defaultValue = AdminConstants.STATUS_PENDING) String status,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "") String keyword,
                         RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    songRequestAdminService.reject(id, reason);
                    adminLogService.log(AdminAction.SONG_REQUEST_REJECT, "SONG_REQUEST", id, reason);
                },
                "노래 요청이 거절되었습니다.",
                e -> log.error("노래 요청 거절 실패 id={}", id, e),
                "거절 처리 중 오류가 발생했습니다.",
                ra);
        return AdminActionUtils.listRedirect("/admin/song-requests", status, page, keyword);
    }

}
