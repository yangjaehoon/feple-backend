package com.feple.feple_backend.artist.photo.controller;

import com.feple.feple_backend.artist.photo.dto.ArtistGalleryPhotoResponseDto;
import com.feple.feple_backend.artist.photo.dto.RegisterPhotoRequestDto;
import com.feple.feple_backend.artist.photo.dto.UpdatePhotoRequestDto;
import com.feple.feple_backend.artist.photo.service.ArtistGalleryPhotoService;
import com.feple.feple_backend.artist.photo.service.ArtistPhotoReportService;
import com.feple.feple_backend.file.dto.PresignResult;
import com.feple.feple_backend.global.exception.AuthenticationRequiredException;
import com.feple.feple_backend.post.dto.SubmitReportCommand;
import com.feple.feple_backend.post.entity.ReportReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/artists/{artistId}/photos")
public class ArtistGalleryPhotoController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final ArtistGalleryPhotoService artistGalleryPhotoService;
    private final ArtistPhotoReportService artistPhotoReportService;

    @PostMapping("/presign")
    public PresignResult presign(
            @PathVariable Long artistId,
            @Valid @RequestBody PresignRequest req,
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new AuthenticationRequiredException("로그인이 필요합니다.");
        }
        String ext = req.extension() == null ? "" : req.extension().toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다. (jpg, jpeg, png, gif, webp 만 가능)");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(req.contentType())) {
            throw new IllegalArgumentException("허용되지 않는 Content-Type입니다.");
        }
        return artistGalleryPhotoService.generateUploadUrl(artistId, ext, req.contentType());
    }

    @PostMapping
    public ArtistGalleryPhotoResponseDto register(
            @PathVariable Long artistId,
            @Valid @RequestBody RegisterPhotoRequestDto req,
            @AuthenticationPrincipal Long userId
    ) {
        return artistGalleryPhotoService.register(artistId, req.objectKey(), req.contentType(), req.title(), req.description(), userId);
    }

    /** 비인증 사용자도 사진 목록 조회 가능 (좋아요 여부는 false로 반환) */
    @GetMapping
    public List<ArtistGalleryPhotoResponseDto> list(
            @PathVariable Long artistId,
            @AuthenticationPrincipal Long userId
    ) {
        return artistGalleryPhotoService.list(artistId, userId);
    }

    public record PresignRequest(
            @NotBlank(message = "Content-Type은 필수입니다.") String contentType,
            @NotBlank(message = "파일 확장자는 필수입니다.") String extension
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
        return artistGalleryPhotoService.update(photoId, userId, req);
    }

    @PostMapping("/{photoId}/report")
    public ResponseEntity<Void> report(
            @PathVariable Long artistId,
            @PathVariable Long photoId,
            @Valid @RequestBody ReportRequest body,
            @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new AuthenticationRequiredException("로그인이 필요합니다.");
        artistPhotoReportService.submitReport(photoId, userId, new SubmitReportCommand(body.reason(), body.detail()));
        return ResponseEntity.ok().build();
    }

    public record ReportRequest(
        @NotNull ReportReason reason,
        String detail
    ) {}

    @PostMapping("/{photoId}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @PathVariable Long photoId,
            @PathVariable Long artistId,
            @AuthenticationPrincipal Long userId) {
        boolean success = artistGalleryPhotoService.toggleLike(photoId, userId);
        return ResponseEntity.ok(Map.of("success", success));
    }
}
