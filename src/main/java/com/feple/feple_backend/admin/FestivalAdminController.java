package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.domain.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequest;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalResponse;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals")
public class FestivalAdminController {

    private final FestivalService festivalService;
    private final FileStorageService fileStorageService;

    private final ArtistRepository artistRepository;
    private final ArtistFestivalService artistFestivalService;

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("festival", new FestivalRequestDto());
        return "admin/festival-form";
    }

    @PostMapping("/new")
    public String createFestival(@ModelAttribute("festival") FestivalRequestDto dto,
                                 @RequestParam(value = "posterFile", required = false) MultipartFile posterFile
    ) throws IOException {

        if (posterFile != null && !posterFile.isEmpty()) {
            String posterUrl = fileStorageService.storeFestivalPoster(posterFile, dto.getStartDate());
            dto.setPosterUrl(posterUrl);
        }

        festivalService.createFestival(dto);
        return "redirect:/admin";

    }

    //목록 페이지
    @GetMapping
    public String listFestivals(
            Model model,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FestivalResponseDto> festivals = festivalService.getAllFestivals(pageable);
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

        model.addAttribute("festivalId", id);
        model.addAttribute("festival", form);
        return "admin/festival-edit-form";
    }

    @PostMapping("/{id}/edit")
    public String updateFestival(@PathVariable Long id,
                                 @ModelAttribute("festival") FestivalRequestDto dto,
                                 @RequestParam(value="posterFile", required=false) MultipartFile posterFile

    )throws IOException {

        String existingPosterUrl = festivalService.getFestival(id).getPosterUrl();
        dto.setPosterUrl(existingPosterUrl);

        if (posterFile != null && !posterFile.isEmpty()) {
            String newPosterUrl = fileStorageService.storeFestivalPoster(posterFile, dto.getStartDate());
            dto.setPosterUrl(newPosterUrl);
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
        List<Artist> allArtists = artistRepository.findAll();

        List<ArtistFestivalResponse> participatingArtists =
                artistFestivalService.getArtistFestivals(id);

        model.addAttribute("festival", festival);
        model.addAttribute("allArtists", allArtists);
        model.addAttribute("participatingArtists", participatingArtists);  // ← 추가


        return "admin/festival-detail";  // 새 템플릿
    }

    @GetMapping("/{id}/artists/new")
    public String addArtistForm(@PathVariable Long id, Model model) {
        FestivalResponseDto festival = festivalService.getFestival(id);
        List<Artist> allArtists = artistRepository.findAll();

        model.addAttribute("festival", festival);
        model.addAttribute("artists", allArtists);
        model.addAttribute("request", new ArtistFestivalCreateRequest());
        return "admin/festival-artist-form";  // 기존 스타일과 맞춤
    }

    @PostMapping("/{id}/artists")
    public String addArtistToFestival(
            @PathVariable Long id,
            ArtistFestivalCreateRequest request) {

        artistFestivalService.addArtistToFestival(id, request);
        return "redirect:/admin/festivals/" + id;
    }

    @GetMapping("/{id}/artists/list")
    @ResponseBody
    public List<ArtistFestivalResponse> getFestivalArtists(@PathVariable Long id) {
        return artistFestivalService.getArtistFestivals(id);
    }

    @PostMapping("/{festivalId}/artists/{artistFestivalId}/delete")
    public String removeArtistFromFestival(
            @PathVariable Long festivalId,
            @PathVariable Long artistFestivalId) {

        artistFestivalService.removeArtistFromFestival(festivalId, artistFestivalId);
        return "redirect:/admin/festivals/" + festivalId;
    }

}
