package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.booth.dto.BoothRequestDto;
import com.feple.feple_backend.booth.service.BoothService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals/{festivalId}/booths")
public class FestivalBoothAdminController {

    private final BoothService boothService;

    @PostMapping
    public String createBooth(@PathVariable Long festivalId,
                              @Valid @ModelAttribute BoothRequestDto dto,
                              BindingResult bindingResult,
                              @RequestParam(value = "boothImageFile", required = false) MultipartFile boothImageFile,
                              RedirectAttributes ra) {
        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return AdminFestivalRedirects.booths(festivalId);
        }
        if (dto.getLatitude() == null || dto.getLongitude() == null) {
            ra.addFlashAttribute("errorMessage", "지도에서 위치를 선택해주세요.");
            return AdminFestivalRedirects.booths(festivalId);
        }
        if (boothImageFile != null && !boothImageFile.isEmpty()) {
            try {
                dto.setImageKey(boothService.uploadBoothImage(boothImageFile));
            } catch (Exception e) {
                log.error("부스 이미지 업로드 실패 festivalId={}", festivalId, e);
                ra.addFlashAttribute("errorMessage", "이미지 업로드에 실패했습니다. 다시 시도해주세요.");
                return AdminFestivalRedirects.booths(festivalId);
            }
        }
        try {
            boothService.createBooth(festivalId, dto);
            ra.addFlashAttribute("successMessage", "부스가 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("부스 추가 실패 festivalId={}", festivalId, e);
            ra.addFlashAttribute("errorMessage", "부스 추가에 실패했습니다.");
        }
        return AdminFestivalRedirects.booths(festivalId);
    }

    @PostMapping("/{boothId}/delete")
    public String deleteBooth(@PathVariable Long festivalId,
                              @PathVariable Long boothId,
                              RedirectAttributes ra) {
        try {
            boothService.deleteBooth(festivalId, boothId);
            ra.addFlashAttribute("successMessage", "부스가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("부스 삭제 실패 festivalId={}, boothId={}", festivalId, boothId, e);
            ra.addFlashAttribute("errorMessage", "부스 삭제에 실패했습니다.");
        }
        return AdminFestivalRedirects.booths(festivalId);
    }


}
