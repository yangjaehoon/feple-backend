package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.song.dto.SaveSongRequestDto;
import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artist.song.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/artists/{artistId}/songs")
@RequiredArgsConstructor
public class ArtistSongAdminController {

    private final SongService songService;
    private final SongAdminService songAdminService;
    private final ArtistService artistService;

    @GetMapping
    public String songsPage(@PathVariable Long artistId,
                            @RequestParam(required = false) String q,
                            Model model) {
        ArtistResponseDto artist = artistService.getArtistById(artistId);
        model.addAttribute("artist", artist);
        model.addAttribute("songs", songService.getSongsByArtistId(artistId));
        model.addAttribute("q", q);
        if (q != null && !q.isBlank()) {
            model.addAttribute("searchResults", songAdminService.searchYoutube(q));
        }
        return "admin/artist-songs";
    }

    @PostMapping
    public String saveSong(@PathVariable Long artistId,
                           @ModelAttribute SaveSongRequestDto dto,
                           RedirectAttributes ra) {
        try {
            songAdminService.saveSong(artistId, dto);
            ra.addFlashAttribute("successMessage", "'" + dto.getTitle() + "' 곡이 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/artists/" + artistId + "/songs";
    }

    @PostMapping("/{songId}/delete")
    public String deleteSong(@PathVariable Long artistId,
                             @PathVariable Long songId,
                             RedirectAttributes ra) {
        songAdminService.deleteSong(songId);
        ra.addFlashAttribute("successMessage", "곡이 삭제되었습니다.");
        return "redirect:/admin/artists/" + artistId + "/songs";
    }
}
