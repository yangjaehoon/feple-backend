package com.feple.feple_backend.artist.photo;

import com.feple.feple_backend.artist.entity.ArtistPhotoLike;
import com.feple.feple_backend.artist.photo.like.ArtistPhotoLikeRepository;
import com.feple.feple_backend.artist.service.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtistPhotoService {

    private final ArtistPhotoRepository artistPhotoRepository;
    private final S3PresignService s3PresignService;
    private final ArtistPhotoLikeRepository artistPhotoLikeRepository;


    public ArtistPhotoResponseDto register(
            Long artistId,
            String objectKey,
            String contentType,
            String title,
            String description) {

        var auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("principal class = " + auth.getPrincipal().getClass());
        System.out.println("principal value = " + auth.getPrincipal());
        System.out.println("name = " + auth.getName());
        System.out.println("authorities = " + auth.getAuthorities());

        String prefix = "artist-photos/" + artistId + "/";
        if (objectKey == null || !objectKey.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid objectKey");
        }

        String principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        Long userId = Long.parseLong(principal);

        ArtistPhoto saved = artistPhotoRepository.save(
                new ArtistPhoto(artistId, userId, objectKey, contentType, title, description)
        );

        String url = s3PresignService.presignGetUrl(saved.getS3Key());
        return new ArtistPhotoResponseDto(
                saved.getId(),
                url,
                saved.getUploaderUserId(),
                saved.getCreatedAt(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getLikecount(),
                false
        );
    }

    public List<ArtistPhotoResponseDto> list(Long artistId, Long currentUserId) {
        return artistPhotoRepository.findByArtistIdOrderByIdDesc(artistId)
                .stream()
                .map(p -> new ArtistPhotoResponseDto(
                        p.getId(),
                        s3PresignService.presignGetUrl(p.getS3Key()),
                        p.getUploaderUserId(),
                        p.getCreatedAt(),
                        p.getTitle(),
                        p.getDescription(),
                        p.getLikecount(),
                        artistPhotoLikeRepository.existsByArtistPhotoIdAndUserId(p.getId(), currentUserId)
                ))
                .toList();
    }


    @Transactional
    public boolean toggleLike(Long photoId, Long userId) {
        ArtistPhoto photo = artistPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        if (artistPhotoLikeRepository.existsByArtistPhotoIdAndUserId(photoId, userId)) {
            // 취소
            artistPhotoLikeRepository.deleteByArtistPhotoIdAndUserId(photoId, userId);
            photo.setLikecount(photo.getLikecount() - 1);
        } else {
            // 추가
            artistPhotoLikeRepository.save(new ArtistPhotoLike(photoId, userId));
            photo.setLikecount(photo.getLikecount() + 1);
        }
        artistPhotoRepository.save(photo);
        return true;
    }
}