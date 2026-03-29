package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.DuplicateArtistFestivalException;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequest;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.file.FileStorageService;
import com.feple.feple_backend.timetable.dto.TimetableEntryRequest;
import com.feple.feple_backend.timetable.service.TimetableService;
import com.feple.feple_backend.stage.service.StageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals")
public class FestivalAdminController {

    private final FestivalService festivalService;
    private final FileStorageService fileStorageService;
    private final ArtistRepository artistRepository;
    private final ArtistFestivalService artistFestivalService;
    private final TimetableService timetableService;
    private final StageService stageService;

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("festival", new FestivalRequestDto());
        model.addAttribute("allArtists", artistRepository.findAll(Sort.by("name")));
        model.addAttribute("allRegions", Region.values());
        model.addAttribute("allGenres", Genre.values());
        return "admin/festival-form";
    }

    @PostMapping("/new")
    public String createFestival(@ModelAttribute("festival") FestivalRequestDto dto,
                                 @RequestParam(value = "posterFile", required = false) MultipartFile posterFile,
                                 @RequestParam(value = "artistIds", required = false) List<Long> artistIds,
                                 Model model) throws IOException {

        List<String> errors = new ArrayList<>();
        if (dto.getTitle() == null || dto.getTitle().isBlank()) errors.add("제목을 입력해주세요.");
        if (dto.getDescription() == null || dto.getDescription().isBlank()) errors.add("설명을 입력해주세요.");

        if (posterFile != null && !posterFile.isEmpty()) {
            try {
                String posterUrl = fileStorageService.storeFestivalPoster(posterFile, dto.getStartDate());
                dto.setPosterUrl(posterUrl);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("allArtists", artistRepository.findAll(Sort.by("name")));
            model.addAttribute("allRegions", Region.values());
            model.addAttribute("allGenres", Genre.values());
            return "admin/festival-form";
        }

        Long festivalId = festivalService.createFestival(dto);

        if (artistIds != null) {
            for (Long artistId : artistIds) {
                try {
                    ArtistFestivalCreateRequest req = new ArtistFestivalCreateRequest();
                    req.setArtistId(artistId);
                    artistFestivalService.addArtistToFestival(festivalId, req);
                } catch (DuplicateArtistFestivalException ignored) {}
            }
        }

        return "redirect:/admin";
    }

    //목록 페이지
    @GetMapping
    public String listFestivals(
            Model model,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        List<FestivalResponseDto> festivals = festivalService.getAllFestivals(null, null);
        model.addAttribute("festivals", festivals);
        return "admin/festival-list";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        FestivalResponseDto festival = festivalService.getFestival(id);
        FestivalRequestDto form = new FestivalRequestDto();
        form.setTitle(festival.getTitle());
        form.setDescription(festival.getDescription());
        form.setLocation(festival.getLocation());
        form.setStartDate(festival.getStartDate());
        form.setEndDate(festival.getEndDate());
        form.setPosterUrl(festival.getPosterUrl());
        form.setRegion(festival.getRegion());
        form.setGenres(festival.getGenres());

        model.addAttribute("festivalId", id);
        model.addAttribute("festival", form);
        model.addAttribute("currentPosterUrl", festival.getPosterUrl());
        model.addAttribute("allRegions", Region.values());
        model.addAttribute("allGenres", Genre.values());
        return "admin/festival-edit-form";
    }

    @PostMapping("/{id}/edit")
    public String updateFestival(@PathVariable Long id,
                                 @ModelAttribute("festival") FestivalRequestDto dto,
                                 @RequestParam(value="posterFile", required=false) MultipartFile posterFile

    )throws IOException {

        if (posterFile != null && !posterFile.isEmpty()) {
            String newPosterKey = fileStorageService.storeFestivalPoster(posterFile, dto.getStartDate());
            dto.setPosterUrl(newPosterKey);
        }
        festivalService.updateFestival(id, dto);
        return "redirect:/admin";
    }

    @PostMapping("/{id}/delete")
    public String deleteFestival(@PathVariable Long id) {
        festivalService.deleteFestival(id);
        return "redirect:/admin";
    }

    //참여 아티스트 관리 페이지
    @GetMapping("/{id}")
    public String festivalDetail(@PathVariable Long id, Model model) {
        FestivalResponseDto festival = festivalService.getFestival(id);
        List<Artist> allArtists = artistRepository.findAll(Sort.by("name"));

        List<ArtistFestivalResponse> participatingArtists =
                artistFestivalService.getArtistFestivals(id);

        model.addAttribute("festival", festival);
        model.addAttribute("allArtists", allArtists);
        model.addAttribute("participatingArtists", participatingArtists);
        model.addAttribute("timetableEntries", timetableService.getEntries(id));
        model.addAttribute("stages", stageService.getStages(id));

        return "admin/festival-detail";
    }

    @GetMapping("/{id}/artists/new")
    public String addArtistForm(@PathVariable Long id, Model model) {
        FestivalResponseDto festival = festivalService.getFestival(id);
        List<Artist> allArtists = artistRepository.findAll(Sort.by("name"));

        model.addAttribute("festival", festival);
        model.addAttribute("artists", allArtists);
        model.addAttribute("request", new ArtistFestivalCreateRequest());
        return "admin/festival-artist-form";  // 기존 스타일과 맞춤
    }

    @PostMapping("/{id}/artists")
    public String addArtistToFestival(
            @PathVariable Long id,
            @RequestParam(value = "artistIds", required = false) List<Long> artistIds,
            RedirectAttributes ra) {

        if (artistIds == null || artistIds.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "아티스트를 한 명 이상 선택해주세요.");
            return "redirect:/admin/festivals/" + id + "/artists/new";
        }

        int added = 0, duplicates = 0;
        for (Long artistId : artistIds) {
            try {
                ArtistFestivalCreateRequest req = new ArtistFestivalCreateRequest();
                req.setArtistId(artistId);
                artistFestivalService.addArtistToFestival(id, req);
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
        return "redirect:/admin/festivals/" + id;
    }

    // ── 타임테이블 ─────────────────────────────────────────────────────────────
    @PostMapping("/{id}/timetable")
    public String createTimetableEntry(@PathVariable Long id,
                                       @ModelAttribute TimetableEntryRequest req,
                                       RedirectAttributes ra) {
        try {
            timetableService.createEntry(id, req);
            ra.addFlashAttribute("successMessage", "타임테이블 항목이 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/festivals/" + id;
    }

    @PostMapping("/{id}/timetable/{entryId}/delete")
    public String deleteTimetableEntry(@PathVariable Long id,
                                       @PathVariable Long entryId,
                                       RedirectAttributes ra) {
        timetableService.deleteEntry(entryId);
        ra.addFlashAttribute("successMessage", "항목이 삭제되었습니다.");
        return "redirect:/admin/festivals/" + id;
    }

    @GetMapping("/{id}/artists/list")
    @ResponseBody
    public List<ArtistFestivalResponse> getFestivalArtists(@PathVariable Long id) {
        return artistFestivalService.getArtistFestivals(id);
    }

    @PostMapping("/{festivalId}/artists/{artistFestivalId}/edit")
    public String updateLineup(@PathVariable Long festivalId,
                               @PathVariable Long artistFestivalId,
                               @RequestParam(required = false) Integer lineupOrder,
                               @RequestParam(required = false) String stageName,
                               RedirectAttributes ra) {
        artistFestivalService.updateArtistFestival(festivalId, artistFestivalId, lineupOrder, stageName);
        ra.addFlashAttribute("successMessage", "라인업이 수정되었습니다.");
        return "redirect:/admin/festivals/" + festivalId;
    }

    @PostMapping("/{festivalId}/artists/{artistFestivalId}/delete")
    public String removeArtistFromFestival(
            @PathVariable Long festivalId,
            @PathVariable Long artistFestivalId) {

        artistFestivalService.removeArtistFromFestival(festivalId, artistFestivalId);
        return "redirect:/admin/festivals/" + festivalId;
    }

    // ── 스테이지 관리 ──────────────────────────────────────────────────────────
    @PostMapping("/{id}/stages")
    public String createStage(@PathVariable Long id,
                              @RequestParam String name,
                              RedirectAttributes ra) {
        try {
            stageService.createStage(id, name);
            ra.addFlashAttribute("successMessage", "스테이지가 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/festivals/" + id;
    }

    @PostMapping("/{id}/stages/{stageId}/delete")
    public String deleteStage(@PathVariable Long id,
                              @PathVariable Long stageId,
                              RedirectAttributes ra) {
        stageService.deleteStage(stageId);
        ra.addFlashAttribute("successMessage", "스테이지가 삭제되었습니다.");
        return "redirect:/admin/festivals/" + id;
    }

}
