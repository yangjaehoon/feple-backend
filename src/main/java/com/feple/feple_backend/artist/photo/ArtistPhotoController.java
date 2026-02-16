package com.feple.feple_backend.artist.photo;

import com.feple.feple_backend.artist.service.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/artists/{artistId}/photos")
public class ArtistPhotoController {

    private final S3PresignService s3PresignService;
    private final ArtistPhotoService artistPhotoService;


    @PostMapping("/presign")
    public S3PresignService.PresignResult presign(
            @PathVariable Long artistId,
            @RequestBody PresignRequest req
    ) {
        String objectKey = "artist-photos/" + artistId + "/" + UUID.randomUUID() + "." + req.extension();
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
        return artistPhotoService.list(artistId);
    }

    public record PresignRequest(String contentType, String extension) {}

}