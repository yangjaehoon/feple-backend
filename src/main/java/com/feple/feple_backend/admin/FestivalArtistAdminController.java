package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.global.exception.DuplicateArtistFestivalException;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequest;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals/{festivalId}/artists")
public class FestivalArtistAdminController {

    private final FestivalService festivalService;
    private final ArtistRepository artistRepository;
    private final ArtistFestivalService artistFestivalService;

    @GetMapping("/new")
    public String addArtistForm(@PathVariable Long festivalId, Model model) {
        FestivalResponseDto festival = festivalService.getFestival(festivalId);
        List<Artist> allArtists = artistRepository.findAll(Sort.by("name"));

        model.addAttribute("festival", festival);
        model.addAttribute("artists", allArtists);
        model.addAttribute("request", new ArtistFestivalCreateRequest());
        return "admin/festival-artist-form";
    }

    @PostMapping
    public String addArtistToFestival(
            @PathVariable Long festivalId,
            @RequestParam(value = "artistIds", required = false) List<Long> artistIds,
            RedirectAttributes ra) {

        if (artistIds == null || artistIds.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "아티스트를 한 명 이상 선택해주세요.");
            return "redirect:/admin/festivals/" + festivalId + "/artists/new";
        }

        int added = 0, duplicates = 0;
        for (Long artistId : artistIds) {
            try {
                ArtistFestivalCreateRequest req = new ArtistFestivalCreateRequest();
                req.setArtistId(artistId);
                artistFestivalService.addArtistToFestival(festivalId, req);
                added++;
            } catch (DuplicateArtistFestivalException ignored) {
                duplicates++;
            }
        }

        if (added == 0) {
            ra.addFlashAttribute("errorMessage", "선택한 아티스트가 이미 모두 참여 중입니다.");
        } else if (duplicates > 0) {
            ra.addFlashAttribute("successMessage", added + "명 추가 완료. " + duplicates + "명은 이미 참여 중이었습니다.");
        } else {
            ra.addFlashAttribute("successMessage", added + "명의 아티스트가 추가되었습니다.");
        }
        return "redirect:/admin/festivals/" + festivalId;
    }

    @GetMapping("/list")
    @ResponseBody
    public List<ArtistFestivalResponse> getFestivalArtists(@PathVariable Long festivalId) {
        return artistFestivalService.getArtistFestivals(festivalId);
    }

    @PostMapping("/{artistFestivalId}/edit")
    public String updateLineup(@PathVariable Long festivalId,
                               @PathVariable Long artistFestivalId,
                               @RequestParam(required = false) Integer lineupOrder,
                               @RequestParam(required = false) String stageName,
                               RedirectAttributes ra) {
        artistFestivalService.updateArtistFestival(festivalId, artistFestivalId, lineupOrder, stageName);
        ra.addFlashAttribute("successMessage", "라인업이 수정되었습니다.");
        return "redirect:/admin/festivals/" + festivalId + "#artists";
    }

    @PostMapping("/batch-edit")
    public String batchUpdateLineup(@PathVariable Long festivalId,
                                    @RequestParam("afIds") List<Long> afIds,
                                    @RequestParam("lineupOrders") List<Integer> lineupOrders,
                                    @RequestParam("stageNames") List<String> stageNames,
                                    RedirectAttributes ra) {
        for (int i = 0; i < afIds.size(); i++) {
            String stage = (i < stageNames.size()) ? stageNames.get(i) : null;
            Integer order = (i < lineupOrders.size()) ? lineupOrders.get(i) : null;
            artistFestivalService.updateArtistFestival(festivalId, afIds.get(i), order, stage);
        }
        ra.addFlashAttribute("successMessage", "라인업이 일괄 수정되었습니다.");
        return "redirect:/admin/festivals/" + festivalId + "#artists";
    }

    @PostMapping("/{artistFestivalId}/delete")
    public String removeArtistFromFestival(
            @PathVariable Long festivalId,
            @PathVariable Long artistFestivalId) {

        artistFestivalService.removeArtistFromFestival(festivalId, artistFestivalId);
        return "redirect:/admin/festivals/" + festivalId + "#artists";
    }
}
