package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.photo.dto.ArtistGalleryPhotoResponseDto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoLike;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.service.S3PresignService;
import com.feple.feple_backend.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ArtistGalleryPhotoService {

    private final ArtistGalleryPhotoRepository artistGalleryPhotoRepository;
    private final S3PresignService s3PresignService;
    private final FileStorageService fileStorageService;
    private final ArtistGalleryPhotoLikeRepository artistGalleryPhotoLikeRepository;

    public ArtistGalleryPhotoResponseDto register(
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

        ArtistGalleryPhoto saved = artistGalleryPhotoRepository.save(
                new ArtistGalleryPhoto(artistId, userId, objectKey, contentType, title, description));

        String url = s3PresignService.presignGetUrl(saved.getS3Key());
        return new ArtistGalleryPhotoResponseDto(
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
    public List<ArtistGalleryPhotoResponseDto> list(Long artistId, Long currentUserId) {
        return artistGalleryPhotoRepository.findByArtistIdOrderByIdDesc(artistId)
                .stream()
                .map(p -> new ArtistGalleryPhotoResponseDto(
                        p.getId(),
                        s3PresignService.presignGetUrl(p.getS3Key()),
                        p.getUploaderUserId(),
                        p.getCreatedAt(),
                        p.getTitle(),
                        p.getDescription(),
                        p.getLikeCount(),
                        currentUserId != null && artistGalleryPhotoLikeRepository.existsByArtistPhotoIdAndUserId(p.getId(), currentUserId)))
                .toList();
    }

    @Transactional
    public void delete(Long photoId, Long userId) {
        ArtistGalleryPhoto photo = artistGalleryPhotoRepository.findById(photoId)
                .orElseThrow(() -> new NoSuchElementException("사진을 찾을 수 없습니다."));
        if (!photo.getUploaderUserId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 사진만 삭제할 수 있습니다.");
        }
        String s3Key = photo.getS3Key();
        artistGalleryPhotoRepository.delete(photo);
        fileStorageService.deleteFile(s3Key); // DB 삭제 성공 후 S3 정리
    }

    @Transactional
    public ArtistGalleryPhotoResponseDto update(Long photoId, Long userId, String title, String description) {
        ArtistGalleryPhoto photo = artistGalleryPhotoRepository.findById(photoId)
                .orElseThrow(() -> new NoSuchElementException("사진을 찾을 수 없습니다."));
        if (!photo.getUploaderUserId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 사진만 수정할 수 있습니다.");
        }
        photo.updateTitleAndDescription(title, description);
        String url = s3PresignService.presignGetUrl(photo.getS3Key());
        return new ArtistGalleryPhotoResponseDto(
                photo.getId(), url, photo.getUploaderUserId(), photo.getCreatedAt(),
                photo.getTitle(), photo.getDescription(), photo.getLikeCount(), false);
    }

    @Transactional
    public boolean toggleLike(Long photoId, Long userId) {
        ArtistGalleryPhoto photo = artistGalleryPhotoRepository.findById(photoId)
                .orElseThrow(() -> new NoSuchElementException("사진을 찾을 수 없습니다."));

        if (artistGalleryPhotoLikeRepository.existsByArtistPhotoIdAndUserId(photoId, userId)) {
            // 취소
            artistGalleryPhotoLikeRepository.deleteByArtistPhotoIdAndUserId(photoId, userId);
            artistGalleryPhotoRepository.decrementLikeCount(photoId);
        } else {
            // 추가
            artistGalleryPhotoLikeRepository.save(new ArtistGalleryPhotoLike(photoId, userId));
            artistGalleryPhotoRepository.incrementLikeCount(photoId);
        }
        return true;
    }
}
