package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/artists")
@RequiredArgsConstructor
public class ArtistAdminController {

    private final ArtistService artistService;
    private final ArtistSuggestionAdminService artistSuggestionAdminService;
    private final AdminLogService adminLogService;

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("artist", new ArtistRequestDto());
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
            model.addAttribute("errors", bindingResult.getAllErrors().stream()
                    .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList());
            return "admin/artist/create";
        }

        try {
            dto.setProfileImageKey(artistService.uploadProfile(profileImageFile, dto.getName()));
            artistService.createArtist(dto);
            adminLogService.log("ARTIST_CREATE", "ARTIST", null, dto.getName());
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
                              @RequestParam(required = false) ArtistGenre genre,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        Page<ArtistResponseDto> artistsPage = artistService.getAdminArtistList(sort, keyword, genre, page);
        model.addAttribute("artistsPage", artistsPage);
        model.addAttribute("artists", artistsPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("genre", genre);
        model.addAttribute("allGenres", ArtistGenre.values());
        model.addAttribute("suggestions", artistSuggestionAdminService.getPendingSuggestionsPreview(50));
        model.addAttribute("processedSuggestions", artistSuggestionAdminService.getProcessedSuggestionsPreview(50));
        model.addAttribute("processedSuggestionsTotal", artistSuggestionAdminService.countProcessed());
        return "admin/artist/list";
    }

    @PostMapping("/suggestions/{id}/dismiss")
    public String dismissSuggestion(@PathVariable Long id,
                                    @RequestParam(defaultValue = "") String processNote,
                                    RedirectAttributes ra) {
        try {
            artistSuggestionAdminService.dismiss(id, processNote.isBlank() ? null : processNote.trim());
            adminLogService.log("ARTIST_SUGGESTION_DISMISS", "ARTIST", id, null);
            ra.addFlashAttribute("successMessage", "아티스트 신청이 처리되었습니다.");
        } catch (Exception e) {
            log.error("아티스트 신청 처리 실패: {}", id, e);
            ra.addFlashAttribute("errorMessage", "처리 중 오류가 발생했습니다.");
        }
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
            String imageKey = artistService.uploadProfile(file, artist.getName());
            artistService.updateArtistPhoto(id, imageKey);
            ra.addFlashAttribute("successMessage", "사진이 업데이트되었습니다.");
        } catch (Exception e) {
            log.error("아티스트 프로필 사진 업로드 실패 artistId={}", id, e);
            ra.addFlashAttribute("errorMessage", "사진 업로드에 실패했습니다. 다시 시도해주세요.");
        }
        return "redirect:/admin/artists/photos";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("artistId", id);
            model.addAttribute("artist", artistService.getArtistForEdit(id));
        } catch (java.util.NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 아티스트입니다.");
            return "redirect:/admin/artists";
        }
        return "admin/artist/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateArtist(@PathVariable Long id,
                               @Valid @ModelAttribute("artist") ArtistRequestDto dto,
                               BindingResult bindingResult,
                               @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile,
                               Model model,
                               RedirectAttributes ra
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("artistId", id);
            model.addAttribute("errors", bindingResult.getAllErrors().stream()
                    .map(org.springframework.context.support.DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList());
            return "admin/artist/edit";
        }
        try {
            if (profileImageFile != null && !profileImageFile.isEmpty()) {
                dto.setProfileImageKey(artistService.uploadProfile(profileImageFile, dto.getName()));
            }
            artistService.updateArtist(id, dto);
            adminLogService.log("ARTIST_UPDATE", "ARTIST", id, dto.getName());
            ra.addFlashAttribute("successMessage", "아티스트 정보가 수정되었습니다.");
        } catch (java.util.NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 아티스트입니다.");
        } catch (Exception e) {
            log.error("아티스트 수정 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "수정 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/artists";
    }

    @PostMapping("/batch-name-en")
    public String batchUpdateNameEn(@RequestParam("artistIds") List<Long> artistIds,
                                    @RequestParam("nameEns") List<String> nameEns,
                                    RedirectAttributes ra) {
        try {
            artistService.batchUpdateNameEn(artistIds, nameEns);
            ra.addFlashAttribute("successMessage", "영어 이름이 저장되었습니다.");
        } catch (Exception e) {
            log.error("아티스트 영어 이름 일괄 저장 실패", e);
            ra.addFlashAttribute("errorMessage", "저장 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/artists";
    }

    @PostMapping("/{id}/delete")
    public String deleteArtist(@PathVariable Long id, RedirectAttributes ra) {
        try {
            artistService.deleteArtist(id);
            adminLogService.log("ARTIST_DELETE", "ARTIST", id, null);
        } catch (Exception e) {
            log.error("아티스트 삭제 실패. id={}", id, e);
            ra.addFlashAttribute("errorMessage", "삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/artists";
    }

}
