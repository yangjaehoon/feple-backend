package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.dto.ArtistGalleryPhotoResponseDto;
import com.feple.feple_backend.artist.photo.dto.UpdatePhotoRequestDto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoLike;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.file.S3PathConstants;
import com.feple.feple_backend.file.dto.PresignResult;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityRequirer;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtistGalleryPhotoService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private final ArtistGalleryPhotoRepository artistGalleryPhotoRepository;
    private final S3PresignService s3PresignService;
    private final S3Client s3Client;
    private final FileStorageService fileStorageService;
    private final ArtistGalleryPhotoLikeRepository artistGalleryPhotoLikeRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;

    @Value("${app.s3.bucket}")
    private String bucket;

    public PresignResult generateUploadUrl(Long artistId, String extension, String contentType) {
        String objectKey = S3PathConstants.artistPhotoPrefix(artistId) + UUID.randomUUID() + "." + extension;
        return s3PresignService.presignPut(objectKey, contentType);
    }

    // S3 headObject 조회는 커넥션 점유 없이 수행; 완료 후 각 리포지토리 호출이
    // 자체 트랜잭션으로 DB에 반영한다 (UserServiceImpl.updateProfileImage와 동일 패턴)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ArtistGalleryPhotoResponseDto register(
            Long artistId,
            String objectKey,
            String contentType,
            String title,
            String description,
            boolean isAnonymous,
            Long userId) {

        String prefix = S3PathConstants.artistPhotoPrefix(artistId);
        if (objectKey == null || !objectKey.startsWith(prefix)) {
            throw new IllegalArgumentException("잘못된 오브젝트 키입니다.");
        }

        // presign 단계에서 content-type을 서명에 포함시키지만, 실제 업로드 여부와
        // S3에 저장된 content-type이 허용된 이미지 타입인지 추가로 검증한다.
        verifyS3ImageObject(objectKey);

        Artist artist = EntityRequirer.getOrThrow(artistRepository::findById, artistId, "아티스트");
        User uploader = EntityRequirer.getOrThrow(userRepository::findById, userId, "사용자");

        ArtistGalleryPhoto saved = artistGalleryPhotoRepository.save(
                new ArtistGalleryPhoto(artist, uploader, objectKey, contentType, title, description, isAnonymous));

        String url = s3PresignService.presignGetUrl(saved.getS3Key());
        return ArtistGalleryPhotoResponseDto.from(saved, url, false, userId);
    }

    @Transactional(readOnly = true)
    public List<ArtistGalleryPhotoResponseDto> list(Long artistId, Long currentUserId) {
        List<ArtistGalleryPhoto> photos = artistGalleryPhotoRepository.findByArtist_IdOrderByLikeCountDescCreatedAtDesc(artistId);
        Set<Long> likedPhotoIds = (currentUserId != null && !photos.isEmpty())
                ? artistGalleryPhotoLikeRepository.findLikedPhotoIds(
                        currentUserId, photos.stream().map(ArtistGalleryPhoto::getId).toList())
                : Set.of();
        return photos.stream()
                .map(photo -> ArtistGalleryPhotoResponseDto.from(
                        photo,
                        s3PresignService.presignGetUrl(photo.getS3Key()),
                        likedPhotoIds.contains(photo.getId()),
                        currentUserId))
                .toList();
    }

    @Transactional
    public void delete(Long photoId, Long userId) {
        ArtistGalleryPhoto photo = EntityRequirer.getOrThrow(artistGalleryPhotoRepository::findById, photoId, "사진");
        if (!photo.getUploaderId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 사진만 삭제할 수 있습니다.");
        }
        String s3Key = photo.getS3Key();
        artistGalleryPhotoLikeRepository.deleteByPhotoId(photoId);
        artistGalleryPhotoRepository.delete(photo);
        fileStorageService.deleteFileAfterCommit(s3Key);
    }

    @Transactional
    public void update(Long photoId, Long userId, UpdatePhotoRequestDto command) {
        ArtistGalleryPhoto photo = EntityRequirer.getOrThrow(artistGalleryPhotoRepository::findById, photoId, "사진");
        if (!photo.getUploaderId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 사진만 수정할 수 있습니다.");
        }
        photo.updateTitleAndDescription(command.title(), command.description());
    }

    @Transactional(readOnly = true)
    public ArtistGalleryPhotoResponseDto getPhoto(Long photoId, Long currentUserId) {
        ArtistGalleryPhoto photo = EntityRequirer.getOrThrow(artistGalleryPhotoRepository::findById, photoId, "사진");
        String url = s3PresignService.presignGetUrl(photo.getS3Key());
        boolean isLiked = currentUserId != null &&
                artistGalleryPhotoLikeRepository.existsByPhoto_IdAndUser_Id(photoId, currentUserId);
        return ArtistGalleryPhotoResponseDto.from(photo, url, isLiked, currentUserId);
    }

    private void verifyS3ImageObject(String objectKey) {
        HeadObjectResponse head;
        try {
            head = s3Client.headObject(r -> r.bucket(bucket).key(objectKey));
        } catch (NoSuchKeyException e) {
            throw new IllegalArgumentException("업로드된 파일을 찾을 수 없습니다.");
        }
        String ct = head.contentType();
        String baseType = (ct == null) ? "" : ct.split(";")[0].trim();
        if (!ALLOWED_CONTENT_TYPES.contains(baseType)) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식입니다. 이미지 파일만 등록할 수 있습니다.");
        }
    }

    /** 회원 탈퇴 시 해당 유저의 갤러리 사진 좋아요 일괄 제거 */
    @Transactional
    public void removeByUser(Long userId) {
        artistGalleryPhotoLikeRepository.decrementLikeCountByUserId(userId);
        artistGalleryPhotoLikeRepository.deleteByUserId(userId);
    }

    @Transactional
    public boolean toggleLike(Long photoId, Long userId) {
        ArtistGalleryPhoto photo = EntityRequirer.getOrThrow(artistGalleryPhotoRepository::findById, photoId, "사진");
        User user = EntityRequirer.getOrThrow(userRepository::findById, userId, "사용자");

        int deleted = artistGalleryPhotoLikeRepository.deleteByPhotoIdAndUserId(photoId, userId);
        if (deleted > 0) {
            artistGalleryPhotoRepository.decrementLikeCount(photoId);
            return false;
        }
        try {
            artistGalleryPhotoLikeRepository.saveAndFlush(new ArtistGalleryPhotoLike(photo, user));
        } catch (DataIntegrityViolationException ignored) {
            // unique(artist_photo_id, user_id): 동시 요청으로 이미 저장됨
            return true;
        }
        artistGalleryPhotoRepository.incrementLikeCount(photoId);
        return true;
    }
}
