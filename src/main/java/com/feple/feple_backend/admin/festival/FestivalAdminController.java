package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.BindingResultUtils;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.checklist.FestivalChecklistService;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.festival.suggestion.service.FestivalSuggestionAdminService;
import com.feple.feple_backend.global.MusicGenre;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.FESTIVALS)
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals")
public class FestivalAdminController {

    @Value("${app.kakao.maps.key:}")
    private String kakaoMapsKey;

    private final FestivalAdminService festivalService;
    private final ArtistAdminService artistService;
    private final ArtistFestivalService artistFestivalService;
    private final FestivalDetailAggregationService festivalDetailAggregationService;
    private final FestivalChecklistService festivalChecklistService;
    private final FestivalSuggestionAdminService festivalSuggestionAdminService;
    private final AdminLogService adminLogService;

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) String name,
                                 @RequestParam(required = false) Long suggestionId,
                                 Model model) {
        FestivalRequestDto dto = new FestivalRequestDto();
        if (name != null && !name.isBlank()) {
            dto.setTitle(name.trim());
        }
        model.addAttribute("festival", dto);
        model.addAttribute("suggestionId", suggestionId);
        populateFestivalFormModel(model);
        return "admin/festival/create";
    }

    @PostMapping("/new")
    public String createFestival(@Valid @ModelAttribute("festival") FestivalRequestDto dto,
                                 BindingResult bindingResult,
                                 @RequestParam(value = "posterFile", required = false) MultipartFile posterFile,
                                 @RequestParam(value = "artistIds", required = false) List<Long> artistIds,
                                 @RequestParam(required = false) Long suggestionId,
                                 Model model,
                                 RedirectAttributes ra) {

        applyPosterFile(posterFile, dto, bindingResult);

        if (bindingResult.hasErrors()) {
            return renderCreateFormWithError(bindingResult, suggestionId, model);
        }

        try {
            return createFestivalAndLinkArtists(dto, artistIds, suggestionId, ra);
        } catch (IllegalArgumentException e) {
            rejectEndDateError(bindingResult, e);
            return renderCreateFormWithError(bindingResult, suggestionId, model);
        } catch (Exception e) {
            log.error("페스티벌 생성 실패. title={}", dto.getTitle(), e);
            bindingResult.reject("error.create", "생성 중 오류가 발생했습니다.");
            return renderCreateFormWithError(bindingResult, suggestionId, model);
        }
    }

    private String createFestivalAndLinkArtists(FestivalRequestDto dto, List<Long> artistIds, Long suggestionId, RedirectAttributes ra) {
        Long festivalId = festivalService.createFestival(dto);
        adminLogService.log(AdminAction.FESTIVAL_CREATE, "FESTIVAL", festivalId, dto.getTitle());
        resolveSourceSuggestion(suggestionId, festivalId);
        if (!linkArtists(festivalId, artistIds, ra)) {
            return "redirect:/admin/festivals/" + festivalId;
        }
        ra.addFlashAttribute("successMessage", "'" + dto.getTitle() + "' 페스티벌이 등록되었습니다.");
        return "redirect:/admin/festivals/" + festivalId;
    }

    // 페스티벌 신청 목록의 "페스티벌 등록" 링크로 이 폼에 들어온 경우, 생성 성공 시 해당 신청을
    // 자동으로 승인 처리한다 — 관리자가 승인 모달에 ID를 다시 입력하지 않아도 되게 하기 위함.
    // 페스티벌은 이미 생성됐으므로 해소 실패는 경고만 남기고 넘어간다.
    private void resolveSourceSuggestion(Long suggestionId, Long festivalId) {
        if (suggestionId == null) return;
        try {
            festivalSuggestionAdminService.approve(suggestionId, festivalId);
            adminLogService.log(AdminAction.FESTIVAL_SUGGESTION_APPROVE, "FESTIVAL_SUGGESTION", suggestionId, null);
        } catch (Exception e) {
            log.warn("페스티벌 신청 자동 승인 실패: suggestionId={}, festivalId={}", suggestionId, festivalId, e);
        }
    }

    /** 아티스트 연결 성공 여부를 반환한다. 실패해도 페스티벌 자체는 이미 생성된 상태이므로 경고만 남기고 계속 진행한다. */
    private boolean linkArtists(Long festivalId, List<Long> artistIds, RedirectAttributes ra) {
        try {
            artistFestivalService.linkArtistsToFestival(festivalId, artistIds);
            return true;
        } catch (Exception linkEx) {
            log.error("아티스트 연결 실패 festivalId={}", festivalId, linkEx);
            ra.addFlashAttribute("warningMessage", "페스티벌은 등록되었으나 일부 아티스트 연결에 실패했습니다. 상세 탭에서 수동으로 추가해주세요.");
            return false;
        }
    }

    /** 종료일 관련 IllegalArgumentException(생성/수정 공통)을 endDate 필드 에러로 노출한다. */
    private void rejectEndDateError(BindingResult bindingResult, IllegalArgumentException e) {
        bindingResult.rejectValue("endDate", "error.endDate", e.getMessage());
    }

    private String renderCreateFormWithError(BindingResult bindingResult, Long suggestionId, Model model) {
        model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
        model.addAttribute("suggestionId", suggestionId);
        populateFestivalFormModel(model);
        return "admin/festival/create";
    }

    @GetMapping
    public String listFestivals(@RequestParam(defaultValue = "") String keyword,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        Page<FestivalResponseDto> festivalsPage = festivalService.getFestivalsAdminPage(keyword, page, AdminConstants.FESTIVAL_LIST_PAGE_SIZE);

        List<FestivalResponseDto> activeFestivals = festivalService.getAllActiveFestivalsForAdmin();

        model.addAttribute("festivalsPage", festivalsPage);
        model.addAttribute("festivals", activeFestivals);
        model.addAttribute("keyword", keyword);
        model.addAttribute("checklistMap", festivalChecklistService.getChecklistMap());
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
        String currentPosterUrl;
        try {
            currentPosterUrl = festivalService.getFestival(id).getPosterUrl();
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/festivals";
        }
        if (bindingResult.hasErrors()) {
            return renderEditFormWithError(bindingResult, id, currentPosterUrl, model);
        }
        try {
            festivalService.updateFestival(id, dto);
            adminLogService.log(AdminAction.FESTIVAL_UPDATE, "FESTIVAL", id, dto.getTitle());
            ra.addFlashAttribute("successMessage", "페스티벌이 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            rejectEndDateError(bindingResult, e);
            return renderEditFormWithError(bindingResult, id, currentPosterUrl, model);
        } catch (Exception e) {
            log.error("페스티벌 수정 실패. id={}", id, e);
            ra.addFlashAttribute("errorMessage", "수정 중 오류가 발생했습니다.");
            return "redirect:/admin/festivals/" + id;
        }
        return "redirect:/admin/festivals/" + id;
    }

    private String renderEditFormWithError(BindingResult bindingResult, Long id, String currentPosterUrl, Model model) {
        model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
        model.addAttribute("festivalId", id);
        model.addAttribute("currentPosterUrl", currentPosterUrl);
        populateFestivalFormModel(model);
        return "admin/festival/edit";
    }

    @PostMapping("/{id}/delete")
    public String deleteFestival(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    festivalService.deleteFestival(id);
                    adminLogService.log(AdminAction.FESTIVAL_DELETE, "FESTIVAL", id, null);
                },
                "페스티벌이 삭제되었습니다.",
                e -> log.error("페스티벌 삭제 실패. id={}", id, e),
                AdminConstants.MSG_DELETE_ERROR,
                ra);
        return "redirect:/admin/festivals";
    }

    @GetMapping("/deleted")
    public String deletedFestivals(Model model) {
        model.addAttribute("festivals", festivalService.getDeletedFestivals());
        return "admin/festival/deleted";
    }

    @PostMapping("/{id}/restore")
    public String restoreFestival(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    festivalService.restoreFestival(id);
                    adminLogService.log(AdminAction.FESTIVAL_RESTORE, "FESTIVAL", id, null);
                },
                "페스티벌이 복구되었습니다.",
                e -> log.error("페스티벌 복구 실패. id={}", id, e),
                "복구 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/festivals/deleted";
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
        model.addAttribute("allGenres", MusicGenre.values());
        model.addAttribute("allAgeRestrictions", AgeRestriction.values());
        if (kakaoMapsKey == null || kakaoMapsKey.isBlank()) {
            // 브라우저 콘솔에만 에러가 남고 서버 로그엔 흔적이 없어 놓치기 쉬움
            log.warn("[Maps] KAKAO_MAPS_KEY가 설정되지 않아 관리자 페이지 지도가 표시되지 않습니다.");
        }
        model.addAttribute("kakaoMapsKey", kakaoMapsKey);
    }

    @GetMapping("/{id}")
    public String festivalDetail(@PathVariable Long id,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "") String keyword,
                                 Model model, RedirectAttributes ra) {
        return AdminActionUtils.tryRender(
                () -> {
                    FestivalDetailDto detail = festivalDetailAggregationService.getDetail(id);
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
                    model.addAttribute("announcementStageName",          detail.announcementStageName());
                    model.addAttribute("ratingStats",                detail.ratingStats());
                    UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/festivals")
                            .queryParam("page", page);
                    if (!keyword.isBlank()) builder.queryParam("keyword", keyword);
                    model.addAttribute("returnUrl", builder.build().encode().toUriString());
                },
                "admin/festival/detail",
                e -> log.error("페스티벌 상세 조회 실패. id={}", id, e),
                "페스티벌 정보를 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/festivals",
                ra);
    }
}
