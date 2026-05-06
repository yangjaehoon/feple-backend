package com.feple.feple_backend.certification.controller;

import com.feple.feple_backend.artist.service.S3PresignService;
import com.feple.feple_backend.certification.dto.CertificationRequestDto;
import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/certifications")
public class FestivalCertificationController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final FestivalCertificationService certificationService;
    private final S3PresignService s3PresignService;

    @PostMapping("/presign")
    public S3PresignService.PresignResult presign(
            @Valid @RequestBody PresignRequest req,
            @AuthenticationPrincipal Long userId
    ) {
        String ext = req.extension() == null ? "" : req.extension().toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다. (jpg, jpeg, png, webp 만 가능)");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(req.contentType())) {
            throw new IllegalArgumentException("허용되지 않는 Content-Type입니다.");
        }
        String objectKey = "certifications/" + userId + "/" + UUID.randomUUID() + "." + ext;
        return s3PresignService.presignPut(objectKey, req.contentType());
    }

    @PostMapping
    public CertificationResponseDto submit(
            @Valid @RequestBody CertificationRequestDto req,
            @AuthenticationPrincipal Long userId
    ) {
        return certificationService.submit(userId, req.festivalId(), req.photoKey());
    }

    @GetMapping("/my")
    public List<CertificationResponseDto> myCertifications(
            @AuthenticationPrincipal Long userId
    ) {
        return certificationService.getMyCertifications(userId);
    }

    @GetMapping("/my/approved-festivals")
    public List<Long> myApprovedFestivalIds(
            @AuthenticationPrincipal Long userId
    ) {
        return certificationService.getApprovedFestivalIds(userId);
    }

    public record PresignRequest(
            @NotBlank(message = "Content-Type은 필수입니다.") String contentType,
            @NotBlank(message = "파일 확장자는 필수입니다.") String extension
    ) {}
}
