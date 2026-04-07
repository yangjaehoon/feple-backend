package com.feple.feple_backend.artist.photo;

import com.feple.feple_backend.artist.service.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/artists/{artistId}/photos")
public class ArtistPhotoController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final S3PresignService s3PresignService;
    private final ArtistPhotoService artistPhotoService;


    @PostMapping("/presign")
    public S3PresignService.PresignResult presign(
            @PathVariable Long artistId,
            @RequestBody PresignRequest req
    ) {
        String ext = req.extension() == null ? "" : req.extension().toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다. (jpg, jpeg, png, gif, webp 만 가능)");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(req.contentType())) {
            throw new IllegalArgumentException("허용되지 않는 Content-Type입니다.");
        }
        String objectKey = "artist-photos/" + artistId + "/" + UUID.randomUUID() + "." + ext;
        return s3PresignService.presignPut(objectKey, req.contentType());
    }

    @PostMapping
    public ArtistPhotoResponseDto register(
            @PathVariable Long artistId,
            @RequestBody RegisterPhotoRequestDto req
    ) {
        return artistPhotoService.register(artistId, req.objectKey(), req.contentType(), req.title(), req.description());
    }

    @GetMapping
    public List<ArtistPhotoResponseDto> list(@PathVariable Long artistId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return artistPhotoService.list(artistId, userId);  // *** list(artistId, userId) 호출 ***
    }

    public record PresignRequest(String contentType, String extension) {}

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable Long artistId,
            @PathVariable Long photoId) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        artistPhotoService.delete(photoId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{photoId}")
    public ArtistPhotoResponseDto updatePhoto(
            @PathVariable Long artistId,
            @PathVariable Long photoId,
            @RequestBody UpdatePhotoRequestDto req) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return artistPhotoService.update(photoId, userId, req.title(), req.description());
    }

    @PostMapping("/{photoId}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @PathVariable Long photoId,
            @PathVariable Long artistId) {  // artistId 검증용
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean success = artistPhotoService.toggleLike(photoId, userId);
        return ResponseEntity.ok(Map.of("success", success));
    }
}