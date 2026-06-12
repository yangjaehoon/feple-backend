package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.song.dto.SaveSongRequestDto;
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

    @GetMapping
    public String songsPage(@PathVariable Long artistId,
                            @RequestParam(required = false) String videoUrl,
                            Model model,
                            RedirectAttributes ra) {
        try {
            ArtistResponseDto artist = artistService.getArtistById(artistId);
            model.addAttribute("artist", artist);
            model.addAttribute("songs", songService.getSongsByArtistId(artistId));
            model.addAttribute("videoUrl", videoUrl);
            model.addAttribute("pendingRequests", songRequestAdminService.getPendingRequests(artistId));
            if (videoUrl != null && !videoUrl.isBlank()) {
                songAdminService.fetchVideoByUrl(videoUrl)
                        .ifPresent(v -> model.addAttribute("previewVideo", v));
            }
        } catch (java.util.NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 아티스트입니다.");
            return "redirect:/admin/artists";
        }
        return "admin/artist/songs";
    }

    @PostMapping
    public String saveSong(@PathVariable Long artistId,
                           @Valid @ModelAttribute SaveSongRequestDto dto,
                           BindingResult bindingResult,
                           RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("errorMessage", firstError(bindingResult));
            return "redirect:/admin/artists/" + artistId + "/songs";
        }
        try {
            songAdminService.saveSong(artistId, dto);
            ra.addFlashAttribute("successMessage", "'" + dto.getTitle() + "' 곡이 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("곡 등록 실패 artistId={}", artistId, e);
            ra.addFlashAttribute("errorMessage", "곡 등록 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/artists/" + artistId + "/songs";
    }

    @PostMapping("/{songId}/delete")
    public String deleteSong(@PathVariable Long artistId,
                             @PathVariable Long songId,
                             RedirectAttributes ra) {
        try {
            songAdminService.deleteSong(artistId, songId);
            ra.addFlashAttribute("successMessage", "곡이 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("곡 삭제 실패 artistId={} songId={}", artistId, songId, e);
            ra.addFlashAttribute("errorMessage", "삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/artists/" + artistId + "/songs";
    }

    @PostMapping("/song-requests/{requestId}/approve")
    public String approveSongRequest(@PathVariable Long artistId,
                                     @PathVariable Long requestId,
                                     @RequestParam(required = false) String youtubeUrl,
                                     RedirectAttributes ra) {
        try {
            boolean songSaved = songRequestAdminService.approve(requestId, youtubeUrl);
            if (songSaved) {
                ra.addFlashAttribute("successMessage", "노래 요청이 승인되었습니다. 곡이 등록되었습니다.");
            } else if (youtubeUrl != null && !youtubeUrl.isBlank()) {
                ra.addFlashAttribute("successMessage", "노래 요청이 승인되었습니다. (YouTube 영상 정보를 가져오지 못해 곡은 등록되지 않았습니다.)");
            } else {
                ra.addFlashAttribute("successMessage", "노래 요청이 승인되었습니다.");
            }
        } catch (Exception e) {
            log.error("노래 요청 승인 실패 requestId={}", requestId, e);
            ra.addFlashAttribute("errorMessage", "노래 요청 승인에 실패했습니다. 다시 시도해주세요.");
        }
        return "redirect:/admin/artists/" + artistId + "/songs";
    }

    @PostMapping("/song-requests/{requestId}/reject")
    public String rejectSongRequest(@PathVariable Long artistId,
                                    @PathVariable Long requestId,
                                    @RequestParam(required = false) String reason,
                                    RedirectAttributes ra) {
        try {
            songRequestAdminService.reject(requestId, reason);
            ra.addFlashAttribute("successMessage", "노래 요청이 거절되었습니다.");
        } catch (Exception e) {
            log.error("노래 요청 거절 실패 requestId={}", requestId, e);
            ra.addFlashAttribute("errorMessage", "노래 요청 거절에 실패했습니다. 다시 시도해주세요.");
        }
        return "redirect:/admin/artists/" + artistId + "/songs";
    }

    private static String firstError(BindingResult br) {
        return br.getAllErrors().get(0).getDefaultMessage();
    }
}
