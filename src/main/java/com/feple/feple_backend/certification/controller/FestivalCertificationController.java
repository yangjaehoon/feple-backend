package com.feple.feple_backend.certification.controller;

import com.feple.feple_backend.certification.dto.CertificationRatingRequestDto;
import com.feple.feple_backend.certification.dto.CertificationRequestDto;
import com.feple.feple_backend.file.dto.PresignResult;
import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "페스티벌 인증", description = "페스티벌 참여 인증 제출·조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/certifications")
public class FestivalCertificationController {

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "webp", "image/webp"
    );

    private final FestivalCertificationService certificationService;

    @PostMapping("/presign")
    public PresignResult presign(
            @Valid @RequestBody PresignRequest req,
            @AuthenticationPrincipal Long userId
    ) {
        String ext = req.extension() == null ? "" : req.extension().toLowerCase();
        if (!req.contentType().equals(ALLOWED_IMAGE_TYPES.get(ext))) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. (jpg, jpeg, png, webp 만 가능)");
        }
        return certificationService.generateUploadUrl(userId, ext, req.contentType());
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CertificationResponseDto submit(
            @Valid @RequestBody CertificationRequestDto req,
            @AuthenticationPrincipal Long userId
    ) {
        return certificationService.submit(userId, req.festivalId(), req.photoKey());
    }

    @GetMapping
    public List<CertificationResponseDto> myCertifications(
            @AuthenticationPrincipal Long userId
    ) {
        return certificationService.getMyCertifications(userId);
    }

    @GetMapping("/approved-festivals")
    public List<Long> myApprovedFestivalIds(
            @AuthenticationPrincipal Long userId
    ) {
        return certificationService.getApprovedFestivalIds(userId);
    }

    @GetMapping("/cert-state")
    public Map<String, Object> getMyCertState(
            @RequestParam Long festivalId,
            @AuthenticationPrincipal Long userId
    ) {
        return certificationService.getCertDetail(userId, festivalId);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{id}/rating")
    public void submitRating(
            @PathVariable Long id,
            @Valid @RequestBody CertificationRatingRequestDto req,
            @AuthenticationPrincipal Long userId
    ) {
        certificationService.submitRating(userId, id, req.rating(), req.review());
    }

    @GetMapping("/festival/{festivalId}/rating")
    public Map<String, Object> getFestivalRating(@PathVariable Long festivalId) {
        return Map.of(
                "averageRating", certificationService.getAverageRating(festivalId),
                "ratingCount", certificationService.getRatingCount(festivalId)
        );
    }

    @GetMapping("/festival/{festivalId}/reviews")
    public Map<String, Object> getFestivalReviews(
            @PathVariable Long festivalId,
            @RequestParam(defaultValue = "0") int page
    ) {
        return certificationService.getFestivalReviewsPage(festivalId, page);
    }

    public record PresignRequest(
            @NotBlank(message = "Content-Type은 필수입니다.") String contentType,
            @NotBlank(message = "파일 확장자는 필수입니다.") String extension
    ) {}
}
