package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.song.entity.ArtistFestivalSong;
import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artist.song.service.SongService;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/artists/{artistId}/setlist")
@RequiredArgsConstructor
public class ArtistSetlistAdminController {

    private final SongService songService;
    private final SongAdminService songAdminService;
    private final ArtistService artistService;
    private final ArtistFestivalService artistFestivalService;

    @GetMapping
    public String setlistIndex(@PathVariable Long artistId, Model model) {
        ArtistResponseDto artist = artistService.getArtistById(artistId);
        List<ArtistFestival> appearances = artistFestivalService.getAppearancesByArtistId(artistId);

        model.addAttribute("artist", artist);
        model.addAttribute("appearances", appearances);
        return "admin/artist-setlist-index";
    }

    @GetMapping("/{artistFestivalId}")
    public String setlistEdit(@PathVariable Long artistId,
                              @PathVariable Long artistFestivalId,
                              @RequestParam(required = false) Long festivalId,
                              Model model) {
        ArtistResponseDto artist = artistService.getArtistById(artistId);
        ArtistFestival artistFestival = artistFestivalService.getArtistFestivalById(artistFestivalId);

        List<ArtistFestivalSong> currentSetlist = songAdminService.getSetlist(artistFestivalId);
        Set<Long> selectedSongIds = new java.util.HashSet<>();
        currentSetlist.forEach(afs -> selectedSongIds.add(afs.getSongId()));

        model.addAttribute("artist", artist);
        model.addAttribute("artistFestival", artistFestival);
        model.addAttribute("songs", songService.getSongsByArtistId(artistId));
        model.addAttribute("selectedSongIds", selectedSongIds);
        model.addAttribute("festivalId", festivalId);
        return "admin/artist-setlist-edit";
    }

    @PostMapping("/{artistFestivalId}")
    public String setlistSave(@PathVariable Long artistId,
                              @PathVariable Long artistFestivalId,
                              @RequestParam(value = "songIds", required = false) Set<Long> songIds,
                              @RequestParam(required = false) Long festivalId,
                              RedirectAttributes ra) {
        songAdminService.saveSetlist(artistFestivalId, songIds != null ? songIds : Set.of());
        ra.addFlashAttribute("successMessage", "셋리스트가 저장되었습니다.");
        if (festivalId != null) {
            return AdminFestivalRedirects.setlist(festivalId);
        }
        return "redirect:/admin/artists/" + artistId + "/setlist";
    }
}
