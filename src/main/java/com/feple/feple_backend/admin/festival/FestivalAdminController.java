package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.FestivalChecklistService;
import com.feple.feple_backend.admin.FestivalDetailAggregationService;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.service.ArtistService;
import lombok.extern.slf4j.Slf4j;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.service.FestivalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.feple.feple_backend.admin.BindingResultUtils;
import com.feple.feple_backend.admin.FestivalDetailModel;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
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
    private final FestivalChecklistService festivalChecklistService;
    private final AdminLogService adminLogService;

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("festival", new FestivalRequestDto());
        populateFestivalFormModel(model);
        return "admin/festival/create";
    }

    @PostMapping("/new")
    public String createFestival(@Valid @ModelAttribute("festival") FestivalRequestDto dto,
                                 BindingResult bindingResult,
                                 @RequestParam(value = "posterFile", required = false) MultipartFile posterFile,
                                 @RequestParam(value = "artistIds", required = false) List<Long> artistIds,
                                 Model model) {

        applyPosterFile(posterFile, dto, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
            populateFestivalFormModel(model);
            return "admin/festival/create";
        }

        try {
            Long festivalId = festivalService.createFestival(dto);
            adminLogService.log(AdminAction.FESTIVAL_CREATE, "FESTIVAL", festivalId, dto.getTitle());
            artistFestivalService.linkArtistsToFestival(festivalId, artistIds);
            return "redirect:/admin/festivals/" + festivalId;
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("endDate", "error.endDate", e.getMessage());
            model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
            populateFestivalFormModel(model);
            return "admin/festival/create";
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String listFestivals(@RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        Page<FestivalResponseDto> festivalsPage = festivalService.getFestivalsAdminPage(keyword, page, 30);

        List<FestivalResponseDto> activeFestivals = festivalService.getAllActiveFestivalsForAdmin();
        List<Long> activeIds = activeFestivals.stream().map(FestivalResponseDto::getId).toList();

        model.addAttribute("festivalsPage", festivalsPage);
        model.addAttribute("festivals", activeFestivals);
        model.addAttribute("keyword", keyword);
        model.addAttribute("checklistMap", festivalChecklistService.getChecklistMap(activeIds));
        model.addAttribute("activeFestivalCount", (long) activeFestivals.size());
        return "admin/festival/list";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return AdminActionUtils.tryRender(
                () -> {
                    FestivalResponseDto festival = festivalService.getFestival(id);
                    model.addAttribute("festivalId", id);
                    model.addAttribute("festival", FestivalRequestDto.from(festival));
                    model.addAttribute("currentPosterUrl", festival.getPosterUrl());
                    populateFestivalFormModel(model);
                },
                "admin/festival/edit",
                e -> log.error("페스티벌 편집 폼 조회 실패. id={}", id, e),
                "페스티벌 정보를 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/festivals",
                ra);
    }

    @PostMapping("/{id}/edit")
    public String updateFestival(@PathVariable Long id,
                                 @Valid @ModelAttribute("festival") FestivalRequestDto dto,
                                 BindingResult bindingResult,
                                 @RequestParam(value="posterFile", required=false) MultipartFile posterFile,
                                 Model model,
                                 RedirectAttributes ra
    ) {
        applyPosterFile(posterFile, dto, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
            model.addAttribute("festivalId", id);
            model.addAttribute("currentPosterUrl", festivalService.getFestival(id).getPosterUrl());
            populateFestivalFormModel(model);
            return "admin/festival/edit";
        }
        try {
            festivalService.updateFestival(id, dto);
            adminLogService.log(AdminAction.FESTIVAL_UPDATE, "FESTIVAL", id, dto.getTitle());
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("endDate", "error.endDate", e.getMessage());
            model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
            model.addAttribute("festivalId", id);
            model.addAttribute("currentPosterUrl", festivalService.getFestival(id).getPosterUrl());
            populateFestivalFormModel(model);
            return "admin/festival/edit";
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/festivals";
        } catch (Exception e) {
            log.error("페스티벌 수정 실패. id={}", id, e);
            ra.addFlashAttribute("errorMessage", "수정 중 오류가 발생했습니다.");
            return "redirect:/admin/festivals/" + id;
        }
        return "redirect:/admin/festivals/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteFestival(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    festivalService.deleteFestival(id);
                    adminLogService.log(AdminAction.FESTIVAL_DELETE, "FESTIVAL", id, null);
                },
                null,
                e -> log.error("페스티벌 삭제 실패. id={}", id, e),
                "삭제 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/festivals";
    }

    private void applyPosterFile(MultipartFile posterFile, FestivalRequestDto dto,
                                  BindingResult bindingResult) {
        if (posterFile == null || posterFile.isEmpty()) return;
        try {
            dto.setPosterKey(festivalService.uploadPosterFile(posterFile, dto.getStartDate()));
        } catch (IllegalArgumentException e) {
            if (bindingResult != null)
                bindingResult.rejectValue("posterKey", "upload.failed", e.getMessage());
        } catch (Exception e) {
            log.error("포스터 업로드 실패", e);
            if (bindingResult != null)
                bindingResult.rejectValue("posterKey", "upload.failed", "포스터 업로드 중 오류가 발생했습니다.");
        }
    }

    private void populateFestivalFormModel(Model model) {
        model.addAttribute("allArtists", artistService.getAllArtistsSortedByName());
        model.addAttribute("allRegions", Region.values());
        model.addAttribute("allGenres", Genre.values());
        model.addAttribute("allAgeRestrictions", AgeRestriction.values());
        model.addAttribute("kakaoMapsKey", kakaoMapsKey);
    }

    @GetMapping("/{id}")
    public String festivalDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return AdminActionUtils.tryRender(
                () -> {
                    FestivalDetailModel detail = festivalDetailAggregationService.getDetail(id);
                    model.addAttribute("festival",                   detail.festival());
                    model.addAttribute("participatingArtists",       detail.participatingArtists());
                    model.addAttribute("participatingArtistsByName", detail.participatingArtistsByName());
                    model.addAttribute("timetableEntries",           detail.timetableEntries());
                    model.addAttribute("timetableByArtist",          detail.timetableByArtist());
                    model.addAttribute("stages",                     detail.stages());
                    model.addAttribute("booths",                     detail.booths());
                    model.addAttribute("allBoothTypes",              detail.allBoothTypes());
                    model.addAttribute("googleMapsKey",              detail.googleMapsKey());
                    model.addAttribute("setlistCounts",              detail.setlistCounts());
                    model.addAttribute("opsStageIndicator",          detail.opsStageIndicator());
                },
                "admin/festival/detail",
                e -> log.error("페스티벌 상세 조회 실패. id={}", id, e),
                "페스티벌 정보를 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/festivals",
                ra);
    }
}
