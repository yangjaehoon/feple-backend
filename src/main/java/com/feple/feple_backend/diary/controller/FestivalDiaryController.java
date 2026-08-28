package com.feple.feple_backend.diary.controller;

import com.feple.feple_backend.diary.dto.CreateDiaryRequestDto;
import com.feple.feple_backend.diary.dto.FestivalDiaryResponseDto;
import com.feple.feple_backend.diary.dto.UpdateDiaryRequestDto;
import com.feple.feple_backend.diary.service.FestivalDiaryService;
import com.feple.feple_backend.file.ImageUploadPolicy;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.global.ValidationMessages;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "페스티벌 일기", description = "페스티벌에 대한 개인 일기(다중 사진+텍스트, 공개/비공개) 작성·조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/diaries")
public class FestivalDiaryController {

    private final FestivalDiaryService diaryService;

    @PostMapping("/presign")
    public S3PresignedUrlResult presign(
            @Valid @RequestBody PresignRequest req,
            @AuthenticationPrincipal Long userId
    ) {
        String ext = ImageUploadPolicy.assertAllowed(req.extension(), req.contentType());
        return diaryService.generateUploadUrl(userId, ext, req.contentType());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public FestivalDiaryResponseDto create(
            @Valid @RequestBody CreateDiaryRequestDto req,
            @AuthenticationPrincipal Long userId
    ) {
        return diaryService.create(userId, req.festivalId(), req);
    }

    @GetMapping("/mine")
    public List<FestivalDiaryResponseDto> myDiaries(
            @RequestParam(required = false) Long festivalId,
            @AuthenticationPrincipal Long userId
    ) {
        return diaryService.getMyDiaries(userId, festivalId);
    }

    @GetMapping("/{id}")
    public FestivalDiaryResponseDto getDiary(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long viewerId = (authentication != null) ? (Long) authentication.getPrincipal() : null;
        return diaryService.getDiary(viewerId, id);
    }

    @PutMapping("/{id}")
    public FestivalDiaryResponseDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDiaryRequestDto req,
            @AuthenticationPrincipal Long userId
    ) {
        return diaryService.update(userId, id, req);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId
    ) {
        diaryService.delete(userId, id);
    }

    @GetMapping("/festival/{festivalId}/public")
    public Page<FestivalDiaryResponseDto> getPublicFeed(
            @PathVariable Long festivalId,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication
    ) {
        Long viewerId = (authentication != null) ? (Long) authentication.getPrincipal() : null;
        return diaryService.getPublicFeed(festivalId, page, viewerId);
    }

    @GetMapping("/user/{userId}/public")
    public Page<FestivalDiaryResponseDto> getUserPublicDiaries(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            Authentication authentication
    ) {
        Long viewerId = (authentication != null) ? (Long) authentication.getPrincipal() : null;
        return diaryService.getUserPublicDiaries(userId, page, viewerId);
    }

    public record PresignRequest(
            @NotBlank(message = ValidationMessages.CONTENT_TYPE_REQUIRED) String contentType,
            @NotBlank(message = ValidationMessages.FILE_EXTENSION_REQUIRED) String extension
    ) {}
}
