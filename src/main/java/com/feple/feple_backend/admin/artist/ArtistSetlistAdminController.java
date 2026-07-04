package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.festival.AdminFestivalRedirects;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
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

import java.util.HashSet;
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
    private final AdminLogService adminLogService;

    @GetMapping
    public String setlistIndex(@PathVariable Long artistId, Model model, RedirectAttributes ra) {
        return AdminActionUtils.tryRender(
                () -> {
                    ArtistResponseDto artist = artistService.getArtistById(artistId);
                    List<ArtistFestival> appearances = artistFestivalService.getAppearancesByArtistId(artistId);
                    model.addAttribute("artist", artist);
                    model.addAttribute("appearances", appearances);
                },
                "admin/artist/setlist/index",
                e -> log.error("셋리스트 목록 조회 실패: artistId={}", artistId, e),
                "정보를 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/artists",
                ra);
    }

    @GetMapping("/{artistFestivalId}")
    public String setlistEdit(@PathVariable Long artistId,
                              @PathVariable Long artistFestivalId,
                              @RequestParam(required = false) Long festivalId,
                              Model model, RedirectAttributes ra) {
        return AdminActionUtils.tryRender(
                () -> {
                    ArtistResponseDto artist = artistService.getArtistById(artistId);
                    ArtistFestival artistFestival = artistFestivalService.getArtistFestivalByIdAndArtistId(artistFestivalId, artistId);

                    List<ArtistFestivalSong> currentSetlist = songAdminService.getSetlist(artistFestivalId);
                    Set<Long> selectedSongIds = new HashSet<>();
                    currentSetlist.forEach(afs -> selectedSongIds.add(afs.getSongId()));

                    model.addAttribute("artist", artist);
                    model.addAttribute("artistFestival", artistFestival);
                    model.addAttribute("songs", songService.getSongsByArtistId(artistId));
                    model.addAttribute("selectedSongIds", selectedSongIds);
                    model.addAttribute("festivalId", festivalId);
                },
                "admin/artist/setlist/edit",
                e -> log.error("셋리스트 편집 조회 실패: artistId={}, afId={}", artistId, artistFestivalId, e),
                "정보를 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/artists",
                ra);
    }

    @PostMapping("/{artistFestivalId}")
    public String setlistSave(@PathVariable Long artistId,
                              @PathVariable Long artistFestivalId,
                              @RequestParam(value = "songIds", required = false) Set<Long> songIds,
                              @RequestParam(required = false) Long festivalId,
                              RedirectAttributes ra) {
        if (!artistFestivalService.existsByIdAndArtistId(artistFestivalId, artistId)) {
            ra.addFlashAttribute("errorMessage", "해당 아티스트의 셋리스트가 아닙니다.");
            return "redirect:/admin/artists/" + artistId + "/setlist";
        }
        Set<Long> ids = songIds != null ? songIds : Set.of();
        AdminActionUtils.tryAction(
                () -> {
                    songAdminService.saveSetlist(artistFestivalId, ids);
                    adminLogService.log(AdminAction.ARTIST_SETLIST_SAVE, "ARTIST", artistId, "afId=" + artistFestivalId + " songs=" + ids.size());
                },
                "셋리스트가 저장되었습니다.",
                e -> log.error("셋리스트 저장 실패: artistId={}, afId={}", artistId, artistFestivalId, e),
                "저장 중 오류가 발생했습니다.",
                ra);
        if (festivalId != null) {
            return AdminFestivalRedirects.setlist(festivalId);
        }
        return "redirect:/admin/artists/" + artistId + "/setlist";
    }
}
