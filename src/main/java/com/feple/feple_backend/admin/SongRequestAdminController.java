package com.feple.feple_backend.admin;

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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.NoSuchElementException;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/song-requests")
@RequiredArgsConstructor
public class SongRequestAdminController {

    private static final int PAGE_SIZE = 20;

    private final SongRequestAdminService songRequestAdminService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "PENDING") String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "") String keyword,
                       Model model) {
        Page<SongRequestResponseDto> requests = songRequestAdminService.getRequestsPage(page, PAGE_SIZE, status, keyword);
        model.addAttribute("requests", requests);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pendingCount", songRequestAdminService.getPendingCount());
        return "admin/song-request-list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String youtubeUrl,
                          @RequestParam(defaultValue = "PENDING") String status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "") String keyword,
                          RedirectAttributes ra) {
        try {
            boolean songSaved = songRequestAdminService.approve(id, youtubeUrl);
            adminLogService.log("SONG_REQUEST_APPROVE", "SONG_REQUEST", id, null);
            if (songSaved) {
                ra.addFlashAttribute("successMessage", "노래 요청이 승인되었습니다. 곡이 등록되었습니다.");
            } else if (youtubeUrl != null && !youtubeUrl.isBlank()) {
                ra.addFlashAttribute("successMessage", "승인되었습니다. (YouTube 영상 정보를 가져오지 못해 곡은 등록되지 않았습니다.)");
            } else {
                ra.addFlashAttribute("successMessage", "노래 요청이 승인되었습니다.");
            }
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("노래 요청 승인 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "승인 처리 중 오류가 발생했습니다.");
        }
        return redirectUrl(status, page, keyword);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         @RequestParam(defaultValue = "PENDING") String status,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "") String keyword,
                         RedirectAttributes ra) {
        try {
            songRequestAdminService.reject(id, reason);
            adminLogService.log("SONG_REQUEST_REJECT", "SONG_REQUEST", id, reason);
            ra.addFlashAttribute("successMessage", "노래 요청이 거절되었습니다.");
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("노래 요청 거절 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "거절 처리 중 오류가 발생했습니다.");
        }
        return redirectUrl(status, page, keyword);
    }

    private String redirectUrl(String status, int page, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/song-requests")
                .queryParam("status", status)
                .queryParam("page", page);
        if (keyword != null && !keyword.isBlank()) builder.queryParam("keyword", keyword);
        return "redirect:" + builder.build().toUriString();
    }
}
