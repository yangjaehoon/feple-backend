package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/artists")
@RequiredArgsConstructor
public class ArtistAdminController {

    private final ArtistService artistService;
    private final FileStorageService fileStorageService;

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("artist", new ArtistRequestDto());
        return "admin/artist-form";
    }

    @PostMapping("/new")
    public String createArtist(@ModelAttribute("artist") ArtistRequestDto dto,
                               @RequestParam(value = "profileImageFile") MultipartFile profileImageFile,
                               org.springframework.validation.BindingResult bindingResult

                               ) throws IOException {

        if (profileImageFile == null || profileImageFile.isEmpty()) {
            bindingResult.rejectValue("profileImageUrl", "profileImageFile.required", "프로필 이미지는 필수입니다.");
            return "admin/artist-form";
        }

        String url = fileStorageService.storeArtistProfile(profileImageFile, dto.getName());
        dto.setProfileImageUrl(url);

        artistService.createArtist(dto);
        return "redirect:/admin/artists";
    }

    @GetMapping
    public String listArtists(Model model) {
        model.addAttribute("artists", artistService.getAllArtists());
        return "admin/artists-list";
    }

    @GetMapping("/photos")
    public String photoManagement(Model model) {
        model.addAttribute("artists", artistService.getAllArtists());
        return "admin/artist-photos";
    }

    @PostMapping("/{id}/photo")
    public String updatePhoto(@PathVariable Long id,
                              @RequestParam("profileImageFile") MultipartFile file,
                              RedirectAttributes ra) throws IOException {
        try {
            String artistName = artistService.getArtistById(id).getName();
            String url = fileStorageService.storeArtistProfile(file, artistName);
            artistService.updateArtistPhoto(id, url);
            ra.addFlashAttribute("successMessage", "사진이 업데이트되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "사진 업로드 실패: " + e.getMessage());
        }
        return "redirect:/admin/artists/photos";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("artistId", id);
        model.addAttribute("artist", artistService.getArtistForEdit(id));
        return "admin/artist-edit-form";
    }

    @PostMapping("/{id}/edit")
    public String updateArtist(@PathVariable Long id,
                               @ModelAttribute("artist") ArtistRequestDto dto,
                               @RequestParam(value = "profileImageFile", required = false) MultipartFile profileImageFile
    ) throws IOException {
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            String url = fileStorageService.storeArtistProfile(profileImageFile, dto.getName());
            dto.setProfileImageUrl(url);
        }
        artistService.updateArtist(id, dto);
        return "redirect:/admin/artists";
    }

    @PostMapping("/{id}/delete")
    public String deleteArtist(@PathVariable Long id) {
        artistService.deleteArtist(id);
        return "redirect:/admin/artists";
    }

}
