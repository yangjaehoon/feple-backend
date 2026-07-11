package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.BindingResultUtils;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.global.MusicGenre;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import jakarta.validation.Valid;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/artists")
@RequiredArgsConstructor
public class ArtistAdminController {

    private final ArtistService artistService;
    private final ArtistAdminService artistAdminService;
    private final ArtistSuggestionAdminService artistSuggestionAdminService;
    private final AdminLogService adminLogService;

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) String name, Model model) {
        ArtistRequestDto dto = new ArtistRequestDto();
        if (name != null && !name.isBlank()) {
            dto.setName(name.trim());
        }
        model.addAttribute("artist", dto);
        return "admin/artist/create";
    }

    @PostMapping("/new")
    public String createArtist(@Valid @ModelAttribute("artist") ArtistRequestDto dto,
                               BindingResult bindingResult,
                               @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
                               Model model,
                               RedirectAttributes ra) {

        if (profileImageFile == null || profileImageFile.isEmpty()) {
            bindingResult.rejectValue("profileImageKey", "profileImageFile.required", "프로필 이미지는 필수입니다.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", BindingResultUtils.extractErrorMessages(bindingResult));
            return "admin/artist/create";
        }

        try {
            dto.setProfileImageKey(artistAdminService.uploadProfile(profileImageFile, dto.getName()));
            Long artistId = artistAdminService.createArtist(dto);
            adminLogService.log(AdminAction.ARTIST_CREATE, "ARTIST", artistId, dto.getName());
            ra.addFlashAttribute("successMessage", "'" + dto.getName() + "' 아티스트가 등록되었습니다.");
        } catch (Exception e) {
            log.error("아티스트 등록 실패 name={}", dto.getName(), e);
            model.addAttribute("errors", List.of("등록 중 오류가 발생했습니다. 다시 시도해주세요."));
            return "admin/artist/create";
        }
        return "redirect:/admin/artists";
    }

    @GetMapping
    public String listArtists(@RequestParam(defaultValue = "") String keyword,
                              @RequestParam(defaultValue = "") String sort,
                              @RequestParam(required = false) MusicGenre genre,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        Page<ArtistResponseDto> artistsPage = artistAdminService.getAdminArtistList(sort, keyword, genre, page);
        model.addAttribute("artistsPage", artistsPage);
        model.addAttribute("artists", artistsPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("genre", genre);
        model.addAttribute("allGenres", MusicGenre.values());
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
        model.addAttribute("artists", artistService.getAllArtists());
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
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "") String sort,
                               Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("artistId", id);
            model.addAttribute("artist", artistAdminService.getArtistForEdit(id));
            model.addAttribute("page", page);
            model.addAttribute("keyword", keyword);
            model.addAttribute("sort", sort);
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/artists";
        }
        return "admin/artist/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateArtist(@PathVariable Long id,
                               @Valid @ModelAttribute("artist") ArtistRequestDto dto,
                               BindingResult bindingResult,
                               @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "") String keyword,
                               @RequestParam(defaultValue = "") String sort,
                               Model model,
                               RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("artistId", id);
            model.addAttribute("page", page);
            model.addAttribute("keyword", keyword);
            model.addAttribute("sort", sort);
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
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/artists").queryParam("page", page);
        if (!keyword.isBlank()) builder.queryParam("keyword", keyword);
        if (!sort.isBlank()) builder.queryParam("sort", sort);
        return "redirect:" + builder.build().toUriString();
    }

    @PostMapping("/batch-name-en")
    public String batchUpdateNameEn(@RequestParam("artistIds") List<Long> artistIds,
                                    @RequestParam("nameEns") List<String> nameEns,
                                    RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    artistAdminService.batchUpdateNameEn(artistIds, nameEns);
                    adminLogService.log(AdminAction.ARTIST_UPDATE, "ARTIST", null, "영어 이름 일괄 수정 " + artistIds.size() + "건");
                },
                "영어 이름이 저장되었습니다.",
                e -> log.error("아티스트 영어 이름 일괄 저장 실패", e),
                "저장 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/artists";
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
                "삭제 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/artists";
    }
}
