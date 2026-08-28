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
import com.feple.feple_backend.file.S3PathConstants;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.file.service.S3ObjectVerificationService;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.LikeToggler;
import com.feple.feple_backend.global.OwnershipValidator;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtistGalleryPhotoService {

    /** 캐러셀 미리보기 limit의 상한 — URL 조작으로 대량 조회·presign을 요청하는 것을 막는다. */
    private static final int PREVIEW_LIMIT_MAX = 50;
    /** 차단한 업로더의 사진이 상위권에 섞여도 요청한 개수를 채우도록 더 조회한 뒤 잘라낼 여유분. */
    private static final int BLOCKED_FILTER_POOL_BUFFER = 10;
    /** limit 미지정(전체 목록) 시에도 폭주를 막는 안전 상한 — 정상 갤러리 규모에서는 도달하지 않는다. */
    private static final int FULL_LIST_MAX = 500;

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
        EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
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

    // limit이 있으면(예: 캐러셀 미리보기) 상위 N개만 조회해 불필요한 presign 서명 비용을 줄인다
    @Transactional(readOnly = true)
    public List<ArtistGalleryPhotoResponseDto> list(Long artistId, Long currentUserId, Integer limit) {
        List<ArtistGalleryPhoto> visiblePhotos = (limit != null)
                ? loadPreview(artistId, currentUserId, clampPreviewLimit(limit))
                : blockedContentFilter.excludeBlocked(
                        artistGalleryPhotoRepository.findByArtist_IdOrderByLikeCountDescCreatedAtDesc(
                                artistId, PageRequest.of(0, FULL_LIST_MAX)),
                        currentUserId, ArtistGalleryPhoto::getUploaderId);
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

    private static int clampPreviewLimit(int limit) {
        return Math.max(1, Math.min(limit, PREVIEW_LIMIT_MAX));
    }

    // 차단 필터가 상위 N개를 깎아 요청 개수보다 적게 노출되는 것을 막기 위해 여유분을 더 조회한 뒤
    // 필터링하고 정확히 N개로 자른다 (PostServiceImpl.getPopularPosts의 pool 방식과 동일).
    private List<ArtistGalleryPhoto> loadPreview(Long artistId, Long currentUserId, int limit) {
        List<ArtistGalleryPhoto> pool = artistGalleryPhotoRepository.findByArtist_IdOrderByLikeCountDescCreatedAtDesc(
                artistId, PageRequest.of(0, limit + BLOCKED_FILTER_POOL_BUFFER));
        return blockedContentFilter.excludeBlocked(pool, currentUserId, ArtistGalleryPhoto::getUploaderId)
                .stream().limit(limit).toList();
    }

    @Transactional
    public void delete(Long photoId, Long userId) {
        ArtistGalleryPhoto photo = EntityLoader.getOrThrow(artistGalleryPhotoRepository::findById, photoId, "사진");
        OwnershipValidator.checkOwner(photo.getUploaderId(), userId, "사진");
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
        OwnershipValidator.checkOwner(photo.getUploaderId(), userId, "사진", "수정");
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
