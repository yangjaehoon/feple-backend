package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.BindingResultUtils;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.ocr.UnmatchedArtistSuggestionService;
import com.feple.feple_backend.artist.dto.ArtistAdminListQuery;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import com.feple.feple_backend.global.MusicGenre;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.ARTISTS)
@Controller
@RequestMapping("/admin/artists")
@RequiredArgsConstructor
public class ArtistAdminController {

    private final ArtistService artistService;
    private final ArtistAdminService artistAdminService;
    private final ArtistSuggestionAdminService artistSuggestionAdminService;
    private final UnmatchedArtistSuggestionService unmatchedArtistSuggestionService;
    private final AdminLogService adminLogService;

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) String name,
                                 @RequestParam(required = false) Long suggestionId,
                                 @RequestParam(required = false) Long unmatchedSuggestionId,
                                 Model model) {
        ArtistRequestDto dto = new ArtistRequestDto();
        if (name != null && !name.isBlank()) {
            dto.setName(name.trim());
        }
        model.addAttribute("artist", dto);
        addSuggestionRefs(model, suggestionId, unmatchedSuggestionId);
        addGenreOptions(model);
        return "admin/artist/create";
    }

    @PostMapping("/new")
    public String createArtist(@Valid @ModelAttribute("artist") ArtistRequestDto dto,
                               BindingResult bindingResult,
                               @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
                               @RequestParam(required = false) Long suggestionId,
                               @RequestParam(required = false) Long unmatchedSuggestionId,
                               Model model,
                               RedirectAttributes ra) {

        if (profileImageFile == null || profileImageFile.isEmpty()) {
            bindingResult.rejectValue("profileImageKey", "profileImageFile.required", "프로필 이미지는 필수입니다.");
        }
        if (bindingResult.hasErrors()) {
            return renderCreateForm(BindingResultUtils.extractErrorMessages(bindingResult),
                    suggestionId, unmatchedSuggestionId, model);
        }

        try {
            dto.setProfileImageKey(artistAdminService.uploadProfile(profileImageFile, dto.getName()));
            Long artistId = artistAdminService.createArtist(dto);
            adminLogService.log(AdminAction.ARTIST_CREATE, "ARTIST", artistId, dto.getName());
            resolveSourceSuggestion(suggestionId, unmatchedSuggestionId, artistId);
            ra.addFlashAttribute("successMessage", "'" + dto.getName() + "' 아티스트가 등록되었습니다.");
        } catch (Exception e) {
            log.error("아티스트 등록 실패 name={}", dto.getName(), e);
            return renderCreateForm(List.of("등록 중 오류가 발생했습니다. 다시 시도해주세요."),
                    suggestionId, unmatchedSuggestionId, model);
        }
        return "redirect:/admin/artists";
    }

    private String renderCreateForm(List<String> errors, Long suggestionId, Long unmatchedSuggestionId, Model model) {
        model.addAttribute("errors", errors);
        addSuggestionRefs(model, suggestionId, unmatchedSuggestionId);
        addGenreOptions(model);
        return "admin/artist/create";
    }

    // 아티스트 신청/미매칭 제안 목록의 "아티스트 등록" 링크로 이 폼에 들어온 경우, 생성 성공 시
    // 그 출처를 자동으로 해소한다 — 관리자가 승인 모달에 ID를 다시 입력하거나 제안을 수동으로
    // 지우지 않아도 되게 하기 위함. 아티스트는 이미 생성됐으므로 해소 실패는 경고만 남기고 넘어간다.
    private void resolveSourceSuggestion(Long suggestionId, Long unmatchedSuggestionId, Long artistId) {
        if (suggestionId != null) {
            try {
                artistSuggestionAdminService.approve(suggestionId, artistId);
                adminLogService.log(AdminAction.ARTIST_SUGGESTION_APPROVE, "ARTIST_SUGGESTION", suggestionId, null);
            } catch (Exception e) {
                log.warn("아티스트 신청 자동 승인 실패: suggestionId={}, artistId={}", suggestionId, artistId, e);
            }
        }
        if (unmatchedSuggestionId != null) {
            try {
                unmatchedArtistSuggestionService.delete(unmatchedSuggestionId);
                adminLogService.log(AdminAction.UNMATCHED_SUGGESTION_DELETE, "UNMATCHED_SUGGESTION", unmatchedSuggestionId, null);
            } catch (Exception e) {
                log.warn("미매칭 아티스트 제안 자동 삭제 실패: id={}", unmatchedSuggestionId, e);
            }
        }
    }

    private static void addSuggestionRefs(Model model, Long suggestionId, Long unmatchedSuggestionId) {
        model.addAttribute("suggestionId", suggestionId);
        model.addAttribute("unmatchedSuggestionId", unmatchedSuggestionId);
    }

    @GetMapping
    public String listArtists(@RequestParam(defaultValue = "") String keyword,
                              @RequestParam(defaultValue = "") String sort,
                              @RequestParam(required = false) MusicGenre genre,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        Page<ArtistResponseDto> artistsPage = artistAdminService.getAdminArtistList(new ArtistAdminListQuery(sort, keyword, genre, page));
        model.addAttribute("artistsPage", artistsPage);
        model.addAttribute("artists", artistsPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("genre", genre);
        addGenreOptions(model);
        model.addAttribute("suggestions", artistSuggestionAdminService.getPendingSuggestionsPreview(AdminConstants.SUGGESTION_PREVIEW_SIZE));
        model.addAttribute("processedSuggestions", artistSuggestionAdminService.getProcessedSuggestionsPreview(AdminConstants.SUGGESTION_PREVIEW_SIZE));
        model.addAttribute("processedSuggestionsTotal", artistSuggestionAdminService.getProcessedCount());
        return "admin/artist/list";
    }

    @PostMapping("/suggestions/{id}/dismiss")
    public String dismissSuggestion(@PathVariable Long id,
                                    @RequestParam(defaultValue = "") String processNote,
                                    RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    artistSuggestionAdminService.dismiss(id, processNote.isBlank() ? null : processNote.trim());
                    adminLogService.log(AdminAction.ARTIST_SUGGESTION_DISMISS, "ARTIST_SUGGESTION", id, null);
                },
                "아티스트 신청이 처리되었습니다.",
                e -> log.error("아티스트 신청 처리 실패: {}", id, e),
                "처리 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/artists";
    }

    @GetMapping("/photos")
    public String photoManagement(Model model) {
        // getArtistRanking()은 weeklyScore 상위 상한(200)까지만 반환해 랭킹 밖 아티스트가
        // 누락될 수 있다 — 사진 관리는 전체 아티스트를 다뤄야 하므로 이름순 전체 목록을 쓴다.
        model.addAttribute("artists", artistAdminService.getAllArtistsSortedByName());
        return "admin/artist/photos";
    }

    @PostMapping("/{id}/photo")
    public String updatePhoto(@PathVariable Long id,
                              @RequestParam("profileImageFile") MultipartFile file,
                              RedirectAttributes ra) {
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "이미지를 선택해주세요.");
            return "redirect:/admin/artists/photos";
        }
        try {
            ArtistResponseDto artist = artistService.getArtistById(id);
            String imageKey = artistAdminService.uploadProfile(file, artist.getName());
            artistAdminService.updateArtistPhoto(id, imageKey);
            adminLogService.log(AdminAction.ARTIST_UPDATE, "ARTIST", id, artist.getName() + " 사진 변경");
            ra.addFlashAttribute("successMessage", "사진이 업데이트되었습니다.");
        } catch (Exception e) {
            log.error("아티스트 프로필 사진 업로드 실패 artistId={}", id, e);
            ra.addFlashAttribute("errorMessage", "사진 업로드에 실패했습니다. 다시 시도해주세요.");
        }
        return "redirect:/admin/artists/photos";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id,
                               @ModelAttribute ArtistListParams params,
                               Model model, RedirectAttributes ra) {
        return AdminActionUtils.tryRender(
                () -> {
                    addEditFormModel(model, id, params);
                    model.addAttribute("artist", artistAdminService.getArtistForEdit(id));
                },
                "admin/artist/edit",
                e -> log.error("아티스트 편집 폼 조회 실패 id={}", id, e),
                "아티스트 정보를 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/artists",
                ra);
    }

    @PostMapping("/{id}/edit")
    public String updateArtist(@PathVariable Long id,
                               @Valid @ModelAttribute("artist") ArtistRequestDto dto,
                               BindingResult bindingResult,
                               @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
                               @ModelAttribute ArtistListParams params,
                               Model model,
                               RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            addEditFormModel(model, id, params);
            model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
            return "admin/artist/edit";
        }
        try {
            if (profileImageFile != null && !profileImageFile.isEmpty()) {
                dto.setProfileImageKey(artistAdminService.uploadProfile(profileImageFile, dto.getName()));
            }
            artistAdminService.updateArtist(id, dto);
            adminLogService.log(AdminAction.ARTIST_UPDATE, "ARTIST", id, dto.getName());
            ra.addFlashAttribute("successMessage", "아티스트 정보가 수정되었습니다.");
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("아티스트 수정 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "수정 중 오류가 발생했습니다.");
        }
        return "redirect:" + params.toRedirectUrl();
    }

    private static void addGenreOptions(Model model) {
        model.addAttribute("allGenres", MusicGenre.values());
    }

    private static void addEditFormModel(Model model, Long id, ArtistListParams params) {
        model.addAttribute("artistId", id);
        model.addAttribute("page", params.page());
        model.addAttribute("keyword", params.keyword());
        model.addAttribute("sort", params.sort());
        addGenreOptions(model);
    }

    @PostMapping("/{id}/delete")
    public String deleteArtist(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    artistAdminService.deleteArtist(id);
                    adminLogService.log(AdminAction.ARTIST_DELETE, "ARTIST", id, null);
                },
                "아티스트가 삭제되었습니다.",
                e -> log.error("아티스트 삭제 실패. id={}", id, e),
                AdminConstants.MSG_DELETE_ERROR,
                ra);
        return "redirect:/admin/artists";
    }

    @GetMapping("/deleted")
    public String deletedArtists(Model model) {
        model.addAttribute("artists", artistAdminService.getDeletedArtists());
        return "admin/artist/deleted";
    }

    @PostMapping("/{id}/restore")
    public String restoreArtist(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    artistAdminService.restoreArtist(id);
                    adminLogService.log(AdminAction.ARTIST_RESTORE, "ARTIST", id, null);
                },
                "아티스트가 복구되었습니다.",
                e -> log.error("아티스트 복구 실패. id={}", id, e),
                "복구 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/artists/deleted";
    }
}
