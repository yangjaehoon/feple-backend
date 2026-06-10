package com.feple.feple_backend.admin;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.data.domain.Page;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
        return "admin/festival-form";
    }

    @PostMapping("/new")
    public String createFestival(@Valid @ModelAttribute("festival") FestivalRequestDto dto,
                                 BindingResult bindingResult,
                                 @RequestParam(value = "posterFile", required = false) MultipartFile posterFile,
                                 @RequestParam(value = "artistIds", required = false) List<Long> artistIds,
                                 Model model) throws IOException {

        applyPosterFile(posterFile, dto, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getAllErrors().stream()
                    .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList());
            populateFestivalFormModel(model);
            return "admin/festival-form";
        }

        Long festivalId = festivalService.createFestival(dto);
        adminLogService.log("FESTIVAL_CREATE", "FESTIVAL", festivalId, dto.getTitle());
        artistFestivalService.linkArtistsToFestival(festivalId, artistIds);

        return "redirect:/admin/festivals/" + festivalId;
    }

    @GetMapping
    public String listFestivals(@RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        // 목록 탭: 페이지네이션 적용
        Page<FestivalResponseDto> festivalsPage = festivalService.getFestivalsAdminPage(keyword, page, 30);

        // 체크리스트 탭: 전체 목록 (종료 포함) — 별도 조회
        LocalDate today = LocalDate.now();
        List<FestivalResponseDto> allFestivals = festivalService.getAllFestivalsForAdmin();
        List<Long> ids = allFestivals.stream().map(FestivalResponseDto::getId).toList();
        long activeFestivalCount = festivalService.countActiveFestivals(today);

        model.addAttribute("festivalsPage", festivalsPage);
        model.addAttribute("festivals", allFestivals);
        model.addAttribute("keyword", keyword);
        model.addAttribute("checklistMap", festivalChecklistService.getChecklistMap(ids));
        model.addAttribute("activeFestivalCount", activeFestivalCount);
        return "admin/festival-list";
    }

    @PostMapping("/{id}/checklist")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleChecklist(@PathVariable Long id,
                                                               @RequestParam String field) {
        festivalChecklistService.toggle(id, field);
        boolean newValue = festivalChecklistService.isChecked(id, field);
        return ResponseEntity.ok(Map.of("checked", newValue));
    }

    @PostMapping("/{id}/checklist/memo")
    @ResponseBody
    public ResponseEntity<Void> saveMemo(@PathVariable Long id,
                                         @RequestParam String memo) {
        festivalChecklistService.saveMemo(id, memo);
        return ResponseEntity.ok().build();
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
                                 @Valid @ModelAttribute("festival") FestivalRequestDto dto,
                                 BindingResult bindingResult,
                                 @RequestParam(value="posterFile", required=false) MultipartFile posterFile,
                                 Model model
    ) throws IOException {
        applyPosterFile(posterFile, dto, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("festivalId", id);
            model.addAttribute("currentPosterUrl", festivalService.getFestival(id).getPosterUrl());
            populateFestivalFormModel(model);
            return "admin/festival-edit-form";
        }
        festivalService.updateFestival(id, dto);
        adminLogService.log("FESTIVAL_UPDATE", "FESTIVAL", id, dto.getTitle());
        return "redirect:/admin/festivals/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteFestival(@PathVariable Long id, RedirectAttributes ra) {
        try {
            festivalService.deleteFestival(id);
            adminLogService.log("FESTIVAL_DELETE", "FESTIVAL", id, null);
        } catch (Exception e) {
            log.error("페스티벌 삭제 실패. id={}", id, e);
            ra.addFlashAttribute("errorMessage", "삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/festivals";
    }

    private void applyPosterFile(MultipartFile posterFile, FestivalRequestDto dto,
                                  BindingResult bindingResult) throws IOException {
        if (posterFile == null || posterFile.isEmpty()) return;
        try {
            dto.setPosterKey(festivalService.uploadPosterFile(posterFile, dto.getStartDate()));
        } catch (IllegalArgumentException e) {
            if (bindingResult != null)
                bindingResult.rejectValue("posterKey", "upload.failed", e.getMessage());
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
        try {
            festivalDetailAggregationService.populateModel(id, model);
        } catch (java.util.NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 페스티벌입니다.");
            return "redirect:/admin/festivals";
        } catch (Exception e) {
            log.error("페스티벌 상세 조회 실패. id={}", id, e);
            ra.addFlashAttribute("errorMessage", "페스티벌 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/admin/festivals";
        }
        return "admin/festival-detail";
    }
}
