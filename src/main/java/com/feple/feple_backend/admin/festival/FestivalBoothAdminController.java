package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.support.AdminActionUtils;
import com.feple.feple_backend.admin.support.BindingResultUtils;
import com.feple.feple_backend.booth.dto.BoothRequestDto;
import com.feple.feple_backend.booth.service.BoothService;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.FESTIVALS)
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/festivals/{festivalId}/booths")
public class FestivalBoothAdminController {

    private final BoothService boothService;
    private final AdminLogService adminLogService;

    @PostMapping
    public String createBooth(@PathVariable Long festivalId,
                              @Valid @ModelAttribute BoothRequestDto dto,
                              BindingResult bindingResult,
                              @RequestParam(value = "boothImageFile", required = false) MultipartFile boothImageFile,
                              RedirectAttributes ra) {
        String formError = validateBoothForm(bindingResult, dto);
        if (formError != null) {
            ra.addFlashAttribute("errorMessage", formError);
            return AdminFestivalRedirects.booths(festivalId);
        }
        String finalImageKey;
        try {
            finalImageKey = uploadBoothImageOrNull(boothImageFile);
        } catch (Exception e) {
            log.error("부스 이미지 업로드 실패 festivalId={}", festivalId, e);
            ra.addFlashAttribute("errorMessage", "이미지 업로드에 실패했습니다. 다시 시도해주세요.");
            return AdminFestivalRedirects.booths(festivalId);
        }
        AdminActionUtils.tryAction(
                () -> {
                    boothService.createBooth(festivalId, dto, finalImageKey);
                    adminLogService.log(AdminAction.FESTIVAL_BOOTH_ADD, "FESTIVAL", festivalId, dto.getName());
                },
                "부스가 추가되었습니다.",
                e -> log.error("부스 추가 실패 festivalId={}", festivalId, e),
                "부스 추가에 실패했습니다.",
                ra);
        return AdminFestivalRedirects.booths(festivalId);
    }

    /** @return 사용자에게 보여줄 오류 메시지, 문제가 없으면 null */
    private String validateBoothForm(BindingResult bindingResult, BoothRequestDto dto) {
        if (bindingResult.hasErrors()) return BindingResultUtils.firstError(bindingResult);
        if (dto.getLatitude() == null || dto.getLongitude() == null) return "지도에서 위치를 선택해주세요.";
        return null;
    }

    /** @return 업로드된 이미지 키, 파일이 없으면 null */
    private String uploadBoothImageOrNull(MultipartFile boothImageFile) throws IOException {
        if (boothImageFile == null || boothImageFile.isEmpty()) return null;
        return boothService.uploadBoothImage(boothImageFile);
    }

    @PostMapping("/{boothId}/delete")
    public String deleteBooth(@PathVariable Long festivalId,
                              @PathVariable Long boothId,
                              RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    boothService.deleteBooth(festivalId, boothId);
                    adminLogService.log(AdminAction.FESTIVAL_BOOTH_DELETE, "FESTIVAL", festivalId, "boothId=" + boothId);
                },
                "부스가 삭제되었습니다.",
                e -> log.error("부스 삭제 실패 festivalId={}, boothId={}", festivalId, boothId, e),
                "부스 삭제에 실패했습니다.",
                ra);
        return AdminFestivalRedirects.booths(festivalId);
    }
}
