package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.global.exception.DuplicateArtistFestivalException;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequest;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals")
public class FestivalAdminController {

    @Value("${app.kakao.maps.key:}")
    private String kakaoMapsKey;

    private final FestivalService festivalService;
    private final ArtistService artistService;
    private final ArtistFestivalService artistFestivalService;
    private final FestivalDetailAggregationService festivalDetailAggregationService;

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("festival", new FestivalRequestDto());
        populateFestivalFormModel(model);
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
                String posterKey = festivalService.uploadPosterFile(posterFile, dto.getStartDate());
                dto.setPosterKey(posterKey);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            populateFestivalFormModel(model);
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

    @GetMapping
    public String listFestivals(Model model) {
        model.addAttribute("festivals", festivalService.getAllFestivals(null, null, true));
        return "admin/festival-list";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        FestivalResponseDto festival = festivalService.getFestival(id);
        model.addAttribute("festivalId", id);
        model.addAttribute("festival", FestivalRequestDto.from(festival));
        model.addAttribute("currentPosterUrl", festival.getPosterUrl());
        populateFestivalFormModel(model);
        return "admin/festival-edit-form";
    }

    @PostMapping("/{id}/edit")
    public String updateFestival(@PathVariable Long id,
                                 @ModelAttribute("festival") FestivalRequestDto dto,
                                 @RequestParam(value="posterFile", required=false) MultipartFile posterFile
    ) throws IOException {
        if (posterFile != null && !posterFile.isEmpty()) {
            String newPosterKey = festivalService.uploadPosterFile(posterFile, dto.getStartDate());
            dto.setPosterKey(newPosterKey);
        }
        festivalService.updateFestival(id, dto);
        return "redirect:/admin";
    }

    @PostMapping("/{id}/delete")
    public String deleteFestival(@PathVariable Long id) {
        festivalService.deleteFestival(id);
        return "redirect:/admin";
    }

    private void populateFestivalFormModel(Model model) {
        model.addAttribute("allArtists", artistService.getAllArtistsSortedByName());
        model.addAttribute("allRegions", Region.values());
        model.addAttribute("allGenres", Genre.values());
        model.addAttribute("kakaoMapsKey", kakaoMapsKey);
    }

    @GetMapping("/{id}")
    public String festivalDetail(@PathVariable Long id, Model model) {
        FestivalDetailDto detail = festivalDetailAggregationService.buildAttributes(id);
        model.addAttribute("festival", detail.festival());
        model.addAttribute("participatingArtists", detail.participatingArtists());
        model.addAttribute("participatingArtistsByName", detail.participatingArtistsByName());
        model.addAttribute("timetableEntries", detail.timetableEntries());
        model.addAttribute("timetableByArtist", detail.timetableByArtist());
        model.addAttribute("stages", detail.stages());
        model.addAttribute("booths", detail.booths());
        model.addAttribute("allBoothTypes", detail.allBoothTypes());
        model.addAttribute("googleMapsKey", detail.googleMapsKey());
        return "admin/festival-detail";
    }
}
