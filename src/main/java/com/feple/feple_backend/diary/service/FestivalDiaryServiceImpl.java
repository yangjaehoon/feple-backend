package com.feple.feple_backend.diary.service;

import com.feple.feple_backend.diary.dto.CreateDiaryRequestDto;
import com.feple.feple_backend.diary.dto.FestivalDiaryResponseDto;
import com.feple.feple_backend.diary.dto.UpdateDiaryRequestDto;
import com.feple.feple_backend.diary.entity.DiaryVisibility;
import com.feple.feple_backend.diary.entity.FestivalDiary;
import com.feple.feple_backend.diary.entity.FestivalDiaryPhoto;
import com.feple.feple_backend.diary.repository.FestivalDiaryPhotoRepository;
import com.feple.feple_backend.diary.repository.FestivalDiaryRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.S3PathConstants;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.file.service.S3ObjectVerificationService;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.OwnershipValidator;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalDiaryServiceImpl implements FestivalDiaryService {

    private static final int PUBLIC_FEED_PAGE_SIZE = 10;

    private final FestivalDiaryRepository diaryRepository;
    private final FestivalDiaryPhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final FestivalRepository festivalRepository;
    private final S3PresignService s3PresignService;
    private final S3ObjectVerificationService s3ObjectVerificationService;
    private final FileStorageService fileStorageService;
    private final BlockedContentFilter blockedContentFilter;

    @Override
    public S3PresignedUrlResult generateUploadUrl(Long userId, String extension, String contentType) {
        String objectKey = S3PathConstants.diaryPhotoPrefix(userId) + UUID.randomUUID() + "." + extension;
        return s3PresignService.presignPut(objectKey, contentType);
    }

    // 사진이 여러 장(부모+자식 다건)이라 S3 검증과 DB 저장을 분리하지 않고 하나의 트랜잭션으로 묶어
    // 원자성을 보장한다 (검증 자체는 HEAD 요청 몇 건이라 커넥션을 오래 점유하지 않음)
    @Override
    @Transactional
    public FestivalDiaryResponseDto create(Long userId, Long festivalId, CreateDiaryRequestDto req) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");

        List<String> photoKeys = req.photoKeys() != null ? req.photoKeys() : List.of();
        for (String photoKey : photoKeys) {
            S3PathConstants.requireWithinPrefix(photoKey, S3PathConstants.diaryPhotoPrefix(userId));
            s3ObjectVerificationService.verifyImageObject(photoKey);
        }

        FestivalDiary diary = FestivalDiary.create(user, festival, req.content(), req.visibility());
        diaryRepository.save(diary);
        savePhotos(diary, photoKeys);

        return toDto(diary, photoKeys, true, null);
    }

    private void savePhotos(FestivalDiary diary, List<String> photoKeys) {
        int order = 0;
        for (String photoKey : photoKeys) {
            photoRepository.save(FestivalDiaryPhoto.create(diary, photoKey, order++));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalDiaryResponseDto> getMyDiaries(Long userId, Long festivalId) {
        List<FestivalDiary> diaries = (festivalId != null)
                ? diaryRepository.findByUserIdAndFestivalIdOrderByCreatedAtDesc(userId, festivalId)
                : diaryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return toDtos(diaries, true, false);
    }

    @Override
    @Transactional(readOnly = true)
    public FestivalDiaryResponseDto getDiary(Long viewerId, Long diaryId) {
        FestivalDiary diary = EntityLoader.getOrThrow(diaryRepository::findById, diaryId, "일기");
        boolean isOwner = diary.getUserId().equals(viewerId);
        if (!diary.isPublic() && !isOwner) {
            throw new AccessDeniedException("비공개 일기입니다.");
        }
        List<String> photoKeys = photoRepository.findByDiaryIdOrderBySortOrder(diaryId).stream()
                .map(FestivalDiaryPhoto::getPhotoKey)
                .toList();
        String authorNickname = isOwner ? null : diary.getUserNickname();
        return toDto(diary, photoKeys, isOwner, authorNickname);
    }

    @Override
    @Transactional
    public FestivalDiaryResponseDto update(Long userId, Long diaryId, UpdateDiaryRequestDto req) {
        FestivalDiary diary = EntityLoader.getOrThrow(diaryRepository::findById, diaryId, "일기");
        OwnershipValidator.checkOwner(diary.getUserId(), userId, "일기", "수정");
        diary.update(req.content(), req.visibility());
        List<String> photoKeys = photoRepository.findByDiaryIdOrderBySortOrder(diaryId).stream()
                .map(FestivalDiaryPhoto::getPhotoKey)
                .toList();
        return toDto(diary, photoKeys, true, null);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long diaryId) {
        FestivalDiary diary = EntityLoader.getOrThrow(diaryRepository::findById, diaryId, "일기");
        OwnershipValidator.checkOwner(diary.getUserId(), userId, "일기");
        photoRepository.findByDiaryIdOrderBySortOrder(diaryId)
                .forEach(photo -> fileStorageService.deleteFileAfterCommit(photo.getPhotoKey()));
        diaryRepository.delete(diary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalDiaryResponseDto> getPublicFeed(Long festivalId, int page, Long viewerId) {
        Page<FestivalDiary> diaryPage = diaryRepository.findByFestivalIdAndVisibilityOrderByCreatedAtDesc(
                festivalId, DiaryVisibility.PUBLIC, PageRequest.of(page, PUBLIC_FEED_PAGE_SIZE));
        List<FestivalDiary> visible = blockedContentFilter.excludeBlocked(diaryPage.getContent(), viewerId, FestivalDiary::getUserId);
        List<FestivalDiaryResponseDto> content = toDtos(visible, false, true);
        return new PageImpl<>(content, diaryPage.getPageable(), diaryPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalDiaryResponseDto> getUserPublicDiaries(Long targetUserId, int page, Long viewerId) {
        Page<FestivalDiary> diaryPage = diaryRepository.findByUserIdAndVisibilityOrderByCreatedAtDesc(
                targetUserId, DiaryVisibility.PUBLIC, PageRequest.of(page, PUBLIC_FEED_PAGE_SIZE));
        boolean isOwner = targetUserId.equals(viewerId);
        List<FestivalDiary> visible = isOwner
                ? diaryPage.getContent()
                : blockedContentFilter.excludeBlocked(diaryPage.getContent(), viewerId, FestivalDiary::getUserId);
        List<FestivalDiaryResponseDto> content = toDtos(visible, isOwner, false);
        return new PageImpl<>(content, diaryPage.getPageable(), diaryPage.getTotalElements());
    }

    @Override
    @Transactional
    public void removeAllByUser(Long userId) {
        List<FestivalDiary> diaries = diaryRepository.findByUserId(userId);
        if (!diaries.isEmpty()) {
            List<Long> diaryIds = diaries.stream().map(FestivalDiary::getId).toList();
            photoRepository.findByDiaryIdIn(diaryIds)
                    .forEach(photo -> fileStorageService.deleteFileAfterCommit(photo.getPhotoKey()));
        }
        diaryRepository.deleteByUserId(userId);
    }

    private List<FestivalDiaryResponseDto> toDtos(List<FestivalDiary> diaries, boolean owner, boolean includeAuthorNickname) {
        if (diaries.isEmpty()) return List.of();
        List<Long> diaryIds = diaries.stream().map(FestivalDiary::getId).toList();
        Map<Long, List<String>> photoKeysByDiaryId = photoRepository.findByDiaryIdIn(diaryIds).stream()
                .collect(Collectors.groupingBy(
                        FestivalDiaryPhoto::getDiaryId,
                        Collectors.mapping(FestivalDiaryPhoto::getPhotoKey, Collectors.toList())));
        return diaries.stream()
                .map(diary -> toDto(
                        diary,
                        photoKeysByDiaryId.getOrDefault(diary.getId(), Collections.emptyList()),
                        owner,
                        includeAuthorNickname ? diary.getUserNickname() : null))
                .toList();
    }

    private FestivalDiaryResponseDto toDto(FestivalDiary diary, List<String> photoKeys, boolean isOwner, String authorNickname) {
        List<String> photoUrls = photoKeys.stream().map(s3PresignService::presignGetUrl).toList();
        return FestivalDiaryResponseDto.of(diary, photoUrls, isOwner, authorNickname);
    }
}
