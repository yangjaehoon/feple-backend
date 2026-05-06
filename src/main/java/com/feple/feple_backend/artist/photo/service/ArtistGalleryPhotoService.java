package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.dto.ArtistGalleryPhotoResponseDto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoLike;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.service.S3PresignService;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ArtistGalleryPhotoService {

    private final ArtistGalleryPhotoRepository artistGalleryPhotoRepository;
    private final S3PresignService s3PresignService;
    private final FileStorageService fileStorageService;
    private final ArtistGalleryPhotoLikeRepository artistGalleryPhotoLikeRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;

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

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new NoSuchElementException("아티스트를 찾을 수 없습니다: " + artistId));
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        ArtistGalleryPhoto saved = artistGalleryPhotoRepository.save(
                new ArtistGalleryPhoto(artist, uploader, objectKey, contentType, title, description));

        String url = s3PresignService.presignGetUrl(saved.getS3Key());
        return new ArtistGalleryPhotoResponseDto(
                saved.getId(),
                url,
                saved.getUploader().getId(),
                saved.getCreatedAt(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getLikeCount(),
                false);
    }

    @Transactional(readOnly = true)
    public List<ArtistGalleryPhotoResponseDto> list(Long artistId, Long currentUserId) {
        List<ArtistGalleryPhoto> photos = artistGalleryPhotoRepository.findByArtist_IdOrderByIdDesc(artistId);
        Set<Long> likedPhotoIds = (currentUserId != null && !photos.isEmpty())
                ? artistGalleryPhotoLikeRepository.findLikedPhotoIds(
                        currentUserId, photos.stream().map(ArtistGalleryPhoto::getId).toList())
                : Set.of();
        return photos.stream()
                .map(p -> new ArtistGalleryPhotoResponseDto(
                        p.getId(),
                        s3PresignService.presignGetUrl(p.getS3Key()),
                        p.getUploader().getId(),
                        p.getCreatedAt(),
                        p.getTitle(),
                        p.getDescription(),
                        p.getLikeCount(),
                        likedPhotoIds.contains(p.getId())))
                .toList();
    }

    @Transactional
    public void delete(Long photoId, Long userId) {
        ArtistGalleryPhoto photo = artistGalleryPhotoRepository.findById(photoId)
                .orElseThrow(() -> new NoSuchElementException("사진을 찾을 수 없습니다."));
        if (!photo.getUploader().getId().equals(userId)) {
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
        if (!photo.getUploader().getId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 사진만 수정할 수 있습니다.");
        }
        photo.updateTitleAndDescription(title, description);
        String url = s3PresignService.presignGetUrl(photo.getS3Key());
        return new ArtistGalleryPhotoResponseDto(
                photo.getId(), url, photo.getUploader().getId(), photo.getCreatedAt(),
                photo.getTitle(), photo.getDescription(), photo.getLikeCount(), false);
    }

    @Transactional
    public boolean toggleLike(Long photoId, Long userId) {
        ArtistGalleryPhoto photo = artistGalleryPhotoRepository.findById(photoId)
                .orElseThrow(() -> new NoSuchElementException("사진을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        if (artistGalleryPhotoLikeRepository.existsByPhoto_IdAndUser_Id(photoId, userId)) {
            // 취소
            artistGalleryPhotoLikeRepository.deleteByPhoto_IdAndUser_Id(photoId, userId);
            artistGalleryPhotoRepository.decrementLikeCount(photoId);
        } else {
            // 추가
            artistGalleryPhotoLikeRepository.save(new ArtistGalleryPhotoLike(photo, user));
            artistGalleryPhotoRepository.incrementLikeCount(photoId);
        }
        return true;
    }
}
