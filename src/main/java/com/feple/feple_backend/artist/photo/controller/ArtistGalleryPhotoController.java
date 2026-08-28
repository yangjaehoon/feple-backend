package com.feple.feple_backend.artist.photo.controller;

import com.feple.feple_backend.artist.photo.dto.ArtistGalleryPhotoResponseDto;
import com.feple.feple_backend.artist.photo.dto.RegisterPhotoRequestDto;
import com.feple.feple_backend.artist.photo.dto.UpdatePhotoRequestDto;
import com.feple.feple_backend.artist.photo.service.ArtistGalleryPhotoService;
import com.feple.feple_backend.artist.photo.service.ArtistPhotoReportService;
import com.feple.feple_backend.file.ImageUploadPolicy;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.global.ValidationMessages;
import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "아티스트 갤러리", description = "아티스트 갤러리 사진 등록·조회·신고")
@RestController
@RequiredArgsConstructor
@RequestMapping("/artists/{artistId}/photos")
public class ArtistGalleryPhotoController {

    private final ArtistGalleryPhotoService artistGalleryPhotoService;
    private final ArtistPhotoReportService artistPhotoReportService;

    @PostMapping("/presign")
    public S3PresignedUrlResult presign(
            @PathVariable Long artistId,
            @Valid @RequestBody PresignRequest req,
            @AuthenticationPrincipal Long userId
    ) {
        String ext = ImageUploadPolicy.assertAllowed(req.extension(), req.contentType());
        return artistGalleryPhotoService.generateUploadUrl(artistId, ext, req.contentType());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ArtistGalleryPhotoResponseDto register(
            @PathVariable Long artistId,
            @Valid @RequestBody RegisterPhotoRequestDto req,
            @AuthenticationPrincipal Long userId
    ) {
        return artistGalleryPhotoService.register(artistId, req, userId);
    }

    /**
     * 비인증 사용자도 사진 목록 조회 가능 (좋아요 여부는 false로 반환)
     * @param limit 지정 시 인기순 상위 N개만 반환 (캐러셀 미리보기 등)
     */
    @GetMapping
    public List<ArtistGalleryPhotoResponseDto> list(
            @PathVariable Long artistId,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal Long userId
    ) {
        return artistGalleryPhotoService.list(artistId, userId, limit);
    }

    public record PresignRequest(
            @NotBlank(message = ValidationMessages.CONTENT_TYPE_REQUIRED) String contentType,
            @NotBlank(message = ValidationMessages.FILE_EXTENSION_REQUIRED) String extension
    ) {}

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long artistId,
            @PathVariable Long photoId,
            @AuthenticationPrincipal Long userId) {
        artistGalleryPhotoService.delete(photoId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{photoId}")
    public ArtistGalleryPhotoResponseDto updatePhoto(
            @PathVariable Long artistId,
            @PathVariable Long photoId,
            @Valid @RequestBody UpdatePhotoRequestDto req,
            @AuthenticationPrincipal Long userId) {
        artistGalleryPhotoService.update(photoId, userId, req);
        return artistGalleryPhotoService.getPhoto(photoId, userId);
    }

    @PostMapping("/{photoId}/report")
    public ResponseEntity<Void> report(
            @PathVariable Long artistId,
            @PathVariable Long photoId,
            @Valid @RequestBody ReportSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        artistPhotoReportService.submitReport(photoId, userId, body);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{photoId}/like")
    public ResponseEntity<Boolean> toggleLike(
            @PathVariable Long photoId,
            @PathVariable Long artistId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(artistGalleryPhotoService.toggleLike(photoId, userId));
    }
}
