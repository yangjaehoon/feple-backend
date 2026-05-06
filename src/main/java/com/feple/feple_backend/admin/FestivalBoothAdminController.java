package com.feple.feple_backend.admin;

import com.feple.feple_backend.booth.dto.BoothRequestDto;
import com.feple.feple_backend.booth.service.BoothService;
import com.feple.feple_backend.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals/{festivalId}/booths")
public class FestivalBoothAdminController {

    private final BoothService boothService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public String createBooth(@PathVariable Long festivalId,
                              @ModelAttribute BoothRequestDto dto,
                              @RequestParam(value = "boothImageFile", required = false) MultipartFile boothImageFile,
                              RedirectAttributes ra) throws IOException {
        if (dto.getLatitude() == null || dto.getLongitude() == null) {
            ra.addFlashAttribute("errorMessage", "지도에서 위치를 선택해주세요.");
            return "redirect:/admin/festivals/" + festivalId + "#booths";
        }
        if (boothImageFile != null && !boothImageFile.isEmpty()) {
            try {
                String key = fileStorageService.storeBoothImage(boothImageFile);
                dto.setImageUrl(fileStorageService.buildUrl(key));
            } catch (Exception e) {
                ra.addFlashAttribute("errorMessage", "이미지 업로드 실패: " + e.getMessage());
                return "redirect:/admin/festivals/" + festivalId + "#booths";
            }
        }
        boothService.createBooth(festivalId, dto);
        ra.addFlashAttribute("successMessage", "부스가 추가되었습니다.");
        return "redirect:/admin/festivals/" + festivalId + "#booths";
    }

    @PostMapping("/{boothId}/delete")
    public String deleteBooth(@PathVariable Long festivalId,
                              @PathVariable Long boothId,
                              RedirectAttributes ra) {
        boothService.deleteBooth(boothId);
        ra.addFlashAttribute("successMessage", "부스가 삭제되었습니다.");
        return "redirect:/admin/festivals/" + festivalId + "#booths";
    }
}
