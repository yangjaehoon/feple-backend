package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.BindingResultUtils;
import com.feple.feple_backend.admin.song.SongApproveMessage;
import java.util.concurrent.atomic.AtomicReference;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.song.dto.SaveSongDto;
import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artist.song.service.SongRequestAdminService;
import com.feple.feple_backend.artist.song.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/artists/{artistId}/songs")
@RequiredArgsConstructor
public class ArtistSongAdminController {

    private final SongService songService;
    private final SongAdminService songAdminService;
    private final ArtistService artistService;
    private final SongRequestAdminService songRequestAdminService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String songsPage(@PathVariable Long artistId,
                            @RequestParam(required = false) String videoUrl,
                            Model model,
                            RedirectAttributes ra) {
        return AdminActionUtils.tryRender(
                () -> {
                    model.addAttribute("artist", artistService.getArtistById(artistId));
                    model.addAttribute("songs", songService.getSongsByArtistId(artistId));
                    model.addAttribute("videoUrl", videoUrl);
                    model.addAttribute("pendingRequests", songRequestAdminService.getPendingRequests(artistId));
                    if (videoUrl != null && !videoUrl.isBlank()) {
                        songAdminService.fetchVideoByUrl(videoUrl)
                                .ifPresent(v -> model.addAttribute("previewVideo", v));
                    }
                },
                "admin/artist/songs",
                e -> log.error("아티스트 곡 목록 조회 실패 artistId={}", artistId, e),
                "곡 목록을 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/artists",
                ra);
    }

    @PostMapping
    public String saveSong(@PathVariable Long artistId,
                           @Valid @ModelAttribute SaveSongDto dto,
                           BindingResult bindingResult,
                           RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("errorMessage", BindingResultUtils.firstError(bindingResult));
            return songsRedirect(artistId);
        }
        AdminActionUtils.tryAction(
                () -> {
                    songAdminService.saveSong(artistId, dto);
                    adminLogService.log(AdminAction.ARTIST_SONG_CREATE, "ARTIST", artistId, dto.getTitle());
                },
                "'" + dto.getTitle() + "' 곡이 등록되었습니다.",
                e -> log.error("곡 등록 실패 artistId={}", artistId, e),
                "곡 등록 중 오류가 발생했습니다.",
                ra);
        return songsRedirect(artistId);
    }

    @PostMapping("/{songId}/delete")
    public String deleteSong(@PathVariable Long artistId,
                             @PathVariable Long songId,
                             RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    songAdminService.deleteSong(artistId, songId);
                    adminLogService.log(AdminAction.ARTIST_SONG_DELETE, "ARTIST", artistId, "songId=" + songId);
                },
                "곡이 삭제되었습니다.",
                e -> log.error("곡 삭제 실패 artistId={} songId={}", artistId, songId, e),
                "삭제 중 오류가 발생했습니다.",
                ra);
        return songsRedirect(artistId);
    }

    @PostMapping("/song-requests/{requestId}/approve")
    public String approveSongRequest(@PathVariable Long artistId,
                                     @PathVariable Long requestId,
                                     @RequestParam(required = false) String youtubeUrl,
                                     RedirectAttributes ra) {
        AtomicReference<String> successMsg = new AtomicReference<>();
        AdminActionUtils.tryAction(
                () -> {
                    boolean songSaved = songRequestAdminService.approve(requestId, youtubeUrl);
                    adminLogService.log(AdminAction.SONG_REQUEST_APPROVE, "SONG_REQUEST", requestId, null);
                    successMsg.set(SongApproveMessage.build(songSaved, youtubeUrl));
                },
                null,
                e -> log.error("노래 요청 승인 실패 requestId={}", requestId, e),
                "노래 요청 승인에 실패했습니다. 다시 시도해주세요.",
                ra);
        if (successMsg.get() != null) ra.addFlashAttribute("successMessage", successMsg.get());
        return songsRedirect(artistId);
    }

    @PostMapping("/song-requests/{requestId}/reject")
    public String rejectSongRequest(@PathVariable Long artistId,
                                    @PathVariable Long requestId,
                                    @RequestParam(required = false) String reason,
                                    RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    songRequestAdminService.reject(requestId, reason);
                    adminLogService.log(AdminAction.SONG_REQUEST_REJECT, "SONG_REQUEST", requestId, reason);
                },
                "노래 요청이 거절되었습니다.",
                e -> log.error("노래 요청 거절 실패 requestId={}", requestId, e),
                "노래 요청 거절에 실패했습니다. 다시 시도해주세요.",
                ra);
        return songsRedirect(artistId);
    }

    private static String songsRedirect(Long artistId) {
        return "redirect:/admin/artists/" + artistId + "/songs";
    }
}
