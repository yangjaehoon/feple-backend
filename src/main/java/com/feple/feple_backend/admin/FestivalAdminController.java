package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.global.exception.DuplicateArtistFestivalException;
import com.feple.feple_backend.artistfestival.dto.ArtistFestivalCreateRequest;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    private final FestivalService festivalService;
    private final FileStorageService fileStorageService;
    private final ArtistRepository artistRepository;
    private final ArtistFestivalService artistFestivalService;
    private final FestivalDetailAggregationService festivalDetailAggregationService;

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
                String posterKey = fileStorageService.storeFestivalPoster(posterFile, dto.getStartDate());
                dto.setPosterKey(posterKey);
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

    @GetMapping
    public String listFestivals(
            Model model,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        List<FestivalResponseDto> festivals = festivalService.getAllFestivals(null, null, true);
        model.addAttribute("festivals", festivals);
        return "admin/festival-list";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        FestivalResponseDto festival = festivalService.getFestival(id);
        FestivalRequestDto form = new FestivalRequestDto();
        form.setTitle(festival.getTitle());
        form.setTitleEn(festival.getTitleEn());
        form.setDescription(festival.getDescription());
        form.setLocation(festival.getLocation());
        form.setStartDate(festival.getStartDate());
        form.setEndDate(festival.getEndDate());
        form.setRegion(festival.getRegion());
        form.setGenres(festival.getGenres());
        form.setLatitude(festival.getLatitude());
        form.setLongitude(festival.getLongitude());

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
    ) throws IOException {
        if (posterFile != null && !posterFile.isEmpty()) {
            String newPosterKey = fileStorageService.storeFestivalPoster(posterFile, dto.getStartDate());
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

    /** 페스티벌 상세: 모든 하위 리소스(아티스트/스테이지/타임테이블/부스) 데이터 집계 */
    @GetMapping("/{id}")
    public String festivalDetail(@PathVariable Long id, Model model) {
        model.addAllAttributes(festivalDetailAggregationService.buildAttributes(id));
        return "admin/festival-detail";
    }
}
