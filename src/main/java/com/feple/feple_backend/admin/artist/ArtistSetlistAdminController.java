package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.festival.AdminFestivalRedirects;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.song.entity.ArtistFestivalSong;
import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artist.song.service.SongService;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Slf4j
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
    public String setlistIndex(@PathVariable Long artistId, Model model, RedirectAttributes ra) {
        try {
            ArtistResponseDto artist = artistService.getArtistById(artistId);
            List<ArtistFestival> appearances = artistFestivalService.getAppearancesByArtistId(artistId);
            model.addAttribute("artist", artist);
            model.addAttribute("appearances", appearances);
        } catch (java.util.NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 아티스트입니다.");
            return "redirect:/admin/artists";
        } catch (Exception e) {
            log.error("셋리스트 목록 조회 실패: artistId={}", artistId, e);
            ra.addFlashAttribute("errorMessage", "정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/admin/artists";
        }
        return "admin/artist/setlist/index";
    }

    @GetMapping("/{artistFestivalId}")
    public String setlistEdit(@PathVariable Long artistId,
                              @PathVariable Long artistFestivalId,
                              @RequestParam(required = false) Long festivalId,
                              Model model, RedirectAttributes ra) {
        try {
            ArtistResponseDto artist = artistService.getArtistById(artistId);
            ArtistFestival artistFestival = artistFestivalService.getArtistFestivalByIdAndArtistId(artistFestivalId, artistId);

            List<ArtistFestivalSong> currentSetlist = songAdminService.getSetlist(artistFestivalId);
            Set<Long> selectedSongIds = new java.util.HashSet<>();
            currentSetlist.forEach(afs -> selectedSongIds.add(afs.getSongId()));

            model.addAttribute("artist", artist);
            model.addAttribute("artistFestival", artistFestival);
            model.addAttribute("songs", songService.getSongsByArtistId(artistId));
            model.addAttribute("selectedSongIds", selectedSongIds);
            model.addAttribute("festivalId", festivalId);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/artists/" + artistId + "/setlist";
        } catch (java.util.NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 아티스트 또는 셋리스트입니다.");
            return "redirect:/admin/artists";
        } catch (Exception e) {
            log.error("셋리스트 편집 조회 실패: artistId={}, afId={}", artistId, artistFestivalId, e);
            ra.addFlashAttribute("errorMessage", "정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/admin/artists";
        }
        return "admin/artist/setlist/edit";
    }

    @PostMapping("/{artistFestivalId}")
    public String setlistSave(@PathVariable Long artistId,
                              @PathVariable Long artistFestivalId,
                              @RequestParam(value = "songIds", required = false) Set<Long> songIds,
                              @RequestParam(required = false) Long festivalId,
                              RedirectAttributes ra) {
        try {
            if (!artistFestivalService.existsByIdAndArtistId(artistFestivalId, artistId)) {
                ra.addFlashAttribute("errorMessage", "해당 아티스트의 셋리스트가 아닙니다.");
                return "redirect:/admin/artists/" + artistId + "/setlist";
            }
            songAdminService.saveSetlist(artistFestivalId, songIds != null ? songIds : Set.of());
            ra.addFlashAttribute("successMessage", "셋리스트가 저장되었습니다.");
        } catch (Exception e) {
            log.error("셋리스트 저장 실패: artistId={}, afId={}", artistId, artistFestivalId, e);
            ra.addFlashAttribute("errorMessage", "저장 중 오류가 발생했습니다.");
        }
        if (festivalId != null) {
            return AdminFestivalRedirects.setlist(festivalId);
        }
        return "redirect:/admin/artists/" + artistId + "/setlist";
    }
}
