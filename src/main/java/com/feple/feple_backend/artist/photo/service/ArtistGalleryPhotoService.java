package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.dto.ArtistGalleryPhotoResponseDto;
import com.feple.feple_backend.artist.photo.dto.RegisterPhotoRequestDto;
import com.feple.feple_backend.artist.photo.dto.UpdatePhotoRequestDto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoLike;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoReportRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.file.S3PathConstants;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.file.service.S3ObjectVerificationService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.LikeToggler;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtistGalleryPhotoService {

    private final ArtistGalleryPhotoRepository artistGalleryPhotoRepository;
    private final S3PresignService s3PresignService;
    private final S3ObjectVerificationService s3ObjectVerificationService;
    private final FileStorageService fileStorageService;
    private final ArtistGalleryPhotoLikeRepository artistGalleryPhotoLikeRepository;
    private final ArtistGalleryPhotoReportRepository artistGalleryPhotoReportRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;
    private final BlockedContentFilter blockedContentFilter;

    public S3PresignedUrlResult generateUploadUrl(Long artistId, String extension, String contentType) {
        String objectKey = S3PathConstants.artistPhotoPrefix(artistId) + UUID.randomUUID() + "." + extension;
        return s3PresignService.presignPut(objectKey, contentType);
    }

    // S3 headObject 조회는 커넥션 점유 없이 수행; 완료 후 각 리포지토리 호출이
    // 자체 트랜잭션으로 DB에 반영한다 (UserServiceImpl.updateProfileImage와 동일 패턴)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ArtistGalleryPhotoResponseDto register(Long artistId, RegisterPhotoRequestDto req, Long userId) {
        String objectKey = req.objectKey();
        S3PathConstants.requireWithinPrefix(objectKey, S3PathConstants.artistPhotoPrefix(artistId));

        // presign 단계에서 content-type을 서명에 포함시키지만, 실제 업로드 여부와
        // S3에 저장된 content-type이 허용된 이미지 타입인지 추가로 검증한다.
        s3ObjectVerificationService.verifyImageObject(objectKey);

        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
        User uploader = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");

        boolean anonymous = Boolean.TRUE.equals(req.isAnonymous());
        ArtistGalleryPhoto saved = artistGalleryPhotoRepository.save(
                new ArtistGalleryPhoto(artist, uploader, objectKey, req.contentType(), req.title(), req.description(), anonymous));

        String url = s3PresignService.presignGetUrl(saved.getS3Key());
        return ArtistGalleryPhotoResponseDto.from(saved, url, false, userId);
    }

    @Transactional(readOnly = true)
    public List<ArtistGalleryPhotoResponseDto> list(Long artistId, Long currentUserId) {
        List<ArtistGalleryPhoto> photos = artistGalleryPhotoRepository.findByArtist_IdOrderByLikeCountDescCreatedAtDesc(artistId);
        // 익명 업로드라도 실제 uploaderId 기준으로 차단을 적용해야 하므로 DTO 변환 전(uploaderUserId가
        // null로 마스킹되기 전) 엔티티 단계에서 필터링한다
        List<ArtistGalleryPhoto> visiblePhotos =
                blockedContentFilter.excludeBlocked(photos, currentUserId, ArtistGalleryPhoto::getUploaderId);
        Set<Long> likedPhotoIds = (currentUserId != null && !visiblePhotos.isEmpty())
                ? artistGalleryPhotoLikeRepository.findLikedPhotoIds(
                        currentUserId, visiblePhotos.stream().map(ArtistGalleryPhoto::getId).toList())
                : Set.of();
        return visiblePhotos.stream()
                .map(photo -> ArtistGalleryPhotoResponseDto.from(
                        photo,
                        s3PresignService.presignGetUrl(photo.getS3Key()),
                        likedPhotoIds.contains(photo.getId()),
                        currentUserId))
                .toList();
    }

    @Transactional
    public void delete(Long photoId, Long userId) {
        ArtistGalleryPhoto photo = EntityLoader.getOrThrow(artistGalleryPhotoRepository::findById, photoId, "사진");
        if (!photo.getUploaderId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 사진만 삭제할 수 있습니다.");
        }
        String s3Key = photo.getS3Key();
        // FK 의존 순서: 신고 → 좋아요 → 사진 (artist_photo_report.photo_id는 ON DELETE 규칙이
        // 없어 미리 정리하지 않으면 사진 삭제 시 제약 위반으로 실패한다)
        artistGalleryPhotoReportRepository.deleteAllByPhotoId(photoId);
        artistGalleryPhotoLikeRepository.deleteByPhotoId(photoId);
        artistGalleryPhotoRepository.delete(photo);
        fileStorageService.deleteFileAfterCommit(s3Key);
    }

    @Transactional
    public void update(Long photoId, Long userId, UpdatePhotoRequestDto command) {
        ArtistGalleryPhoto photo = EntityLoader.getOrThrow(artistGalleryPhotoRepository::findById, photoId, "사진");
        if (!photo.getUploaderId().equals(userId)) {
            throw new IllegalArgumentException("본인이 업로드한 사진만 수정할 수 있습니다.");
        }
        photo.updateTitleAndDescription(command.title(), command.description());
    }

    @Transactional(readOnly = true)
    public ArtistGalleryPhotoResponseDto getPhoto(Long photoId, Long currentUserId) {
        ArtistGalleryPhoto photo = EntityLoader.getOrThrow(artistGalleryPhotoRepository::findById, photoId, "사진");
        String url = s3PresignService.presignGetUrl(photo.getS3Key());
        boolean isLiked = currentUserId != null &&
                artistGalleryPhotoLikeRepository.existsByPhoto_IdAndUser_Id(photoId, currentUserId);
        return ArtistGalleryPhotoResponseDto.from(photo, url, isLiked, currentUserId);
    }

    /** 회원 탈퇴 시 해당 유저의 갤러리 사진 좋아요 일괄 제거 */
    @Transactional
    public void removeByUser(Long userId) {
        artistGalleryPhotoLikeRepository.decrementLikeCountByUserId(userId);
        artistGalleryPhotoLikeRepository.deleteByUserId(userId);
    }

    @Transactional
    public boolean toggleLike(Long photoId, Long userId) {
        ArtistGalleryPhoto photo = EntityLoader.getOrThrow(artistGalleryPhotoRepository::findById, photoId, "사진");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");

        return LikeToggler.toggle(
                () -> artistGalleryPhotoLikeRepository.deleteByPhotoIdAndUserId(photoId, userId),
                () -> artistGalleryPhotoRepository.decrementLikeCount(photoId),
                () -> {
                    artistGalleryPhotoLikeRepository.saveAndFlush(new ArtistGalleryPhotoLike(photo, user));
                    artistGalleryPhotoRepository.incrementLikeCount(photoId);
                });
    }
}
