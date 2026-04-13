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
import java.util.NoSuchElementException;

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
            throw new IllegalArgumentException("잘못된 오브젝트 키입니다.");
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

    @Transactional(readOnly = true)
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
                .orElseThrow(() -> new NoSuchElementException("사진을 찾을 수 없습니다."));
        if (!photo.getUploaderUserId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 사진만 삭제할 수 있습니다.");
        }
        artistPhotoRepository.delete(photo);
    }

    @Transactional
    public ArtistPhotoResponseDto update(Long photoId, Long userId, String title, String description) {
        ArtistPhoto photo = artistPhotoRepository.findById(photoId)
                .orElseThrow(() -> new NoSuchElementException("사진을 찾을 수 없습니다."));
        if (!photo.getUploaderUserId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 사진만 수정할 수 있습니다.");
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
                .orElseThrow(() -> new NoSuchElementException("사진을 찾을 수 없습니다."));

        if (artistPhotoLikeRepository.existsByArtistPhotoIdAndUserId(photoId, userId)) {
            // 취소
            artistPhotoLikeRepository.deleteByArtistPhotoIdAndUserId(photoId, userId);
            artistPhotoRepository.decrementLikeCount(photoId);
        } else {
            // 추가
            artistPhotoLikeRepository.save(new ArtistPhotoLike(photoId, userId));
            artistPhotoRepository.incrementLikeCount(photoId);
        }
        return true;
    }
}