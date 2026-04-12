package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.photo.dto.ArtistPhotoResponseDto;
import com.feple.feple_backend.artist.photo.entity.ArtistPhoto;
import com.feple.feple_backend.artist.photo.entity.ArtistPhotoLike;
import com.feple.feple_backend.artist.photo.repository.ArtistPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistPhotoRepository;
import com.feple.feple_backend.artist.service.S3PresignService;
import lombok.RequiredArgsConstructor;
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
            String description,
            Long userId) {

        String prefix = "artist-photos/" + artistId + "/";
        if (objectKey == null || !objectKey.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid objectKey");
        }

        ArtistPhoto saved = artistPhotoRepository.save(
                new ArtistPhoto(artistId, userId, objectKey, contentType, title, description));

        String url = s3PresignService.presignGetUrl(saved.getS3Key());
        return new ArtistPhotoResponseDto(
                saved.getId(),
                url,
                saved.getUploaderUserId(),
                saved.getCreatedAt(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getLikeCount(),
                false);
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
                        p.getLikeCount(),
                        artistPhotoLikeRepository.existsByArtistPhotoIdAndUserId(p.getId(), currentUserId)))
                .toList();
    }

    @Transactional
    public void delete(Long photoId, Long userId) {
        ArtistPhoto photo = artistPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));
        if (!photo.getUploaderUserId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        artistPhotoRepository.delete(photo);
    }

    @Transactional
    public ArtistPhotoResponseDto update(Long photoId, Long userId, String title, String description) {
        ArtistPhoto photo = artistPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));
        if (!photo.getUploaderUserId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        photo.updateTitleAndDescription(title, description);
        String url = s3PresignService.presignGetUrl(photo.getS3Key());
        return new ArtistPhotoResponseDto(
                photo.getId(), url, photo.getUploaderUserId(), photo.getCreatedAt(),
                photo.getTitle(), photo.getDescription(), photo.getLikeCount(), false);
    }

    @Transactional
    public boolean toggleLike(Long photoId, Long userId) {
        ArtistPhoto photo = artistPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        if (artistPhotoLikeRepository.existsByArtistPhotoIdAndUserId(photoId, userId)) {
            // 취소
            artistPhotoLikeRepository.deleteByArtistPhotoIdAndUserId(photoId, userId);
            photo.decrementLikeCount();
        } else {
            // 추가
            artistPhotoLikeRepository.save(new ArtistPhotoLike(photoId, userId));
            photo.incrementLikeCount();
        }
        return true;
    }
}