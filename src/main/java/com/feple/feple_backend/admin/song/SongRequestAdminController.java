package com.feple.feple_backend.admin.song;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.song.SongApproveMessage;
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

/**
 * 전체 아티스트를 아우르는 노래 신청 관리 페이지("/admin/song-requests") 전용
 * 컨트롤러 — 상태·페이지·키워드로 필터링된 목록을 다룬다. 승인/거절 도메인
 * 로직은 {@link com.feple.feple_backend.admin.artist.ArtistSongAdminController}
 * (아티스트 상세 "곡" 탭)와 동일하게 {@link SongRequestAdminService}에 위임하며,
 * 화면 진입 경로와 리다이렉트 대상(status/page/keyword 기준 vs artistId 기준)이
 * 달라 별도 컨트롤러로 유지한다.
 */
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/song-requests")
@RequiredArgsConstructor
public class SongRequestAdminController {

    private final SongRequestAdminService songRequestAdminService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String list(@ModelAttribute SongListParams params, Model model) {
        Page<SongRequestResponseDto> requests = songRequestAdminService.getRequestsPage(
                params.page(), AdminConstants.LIST_PAGE_SIZE, params.status(), params.keyword());
        model.addAttribute("requests", requests);
        model.addAttribute("status", params.status());
        model.addAttribute("keyword", params.keyword());
        model.addAttribute("pendingCount", songRequestAdminService.getPendingCount());
        return "admin/song-request/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String youtubeUrl,
                          @ModelAttribute SongListParams params,
                          RedirectAttributes ra) {
        AdminActionUtils.tryActionWithResult(
                () -> {
                    boolean songSaved = songRequestAdminService.approveAndMaybeSaveSong(id, youtubeUrl);
                    adminLogService.log(AdminAction.SONG_REQUEST_APPROVE, "SONG_REQUEST", id, null);
                    return songSaved;
                },
                songSaved -> SongApproveMessage.build(songSaved, youtubeUrl),
                e -> log.error("노래 요청 승인 실패 id={}", id, e),
                "승인 처리 중 오류가 발생했습니다.",
                ra);
        return AdminActionUtils.listRedirect("/admin/song-requests", params.status(), params.page(), params.keyword());
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         @ModelAttribute SongListParams params,
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
        return AdminActionUtils.listRedirect("/admin/song-requests", params.status(), params.page(), params.keyword());
    }

}
