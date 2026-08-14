package com.feple.feple_backend.diary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.file.service.S3ObjectVerificationService;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class FestivalDiaryServiceImplTest {

    @Mock FestivalDiaryRepository diaryRepository;
    @Mock FestivalDiaryPhotoRepository photoRepository;
    @Mock UserRepository userRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock S3PresignService s3PresignService;
    @Mock S3ObjectVerificationService s3ObjectVerificationService;
    @Mock FileStorageService fileStorageService;
    @Mock BlockedContentFilter blockedContentFilter;

    @InjectMocks FestivalDiaryServiceImpl diaryService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long FESTIVAL_ID = 2L;
    private static final Long DIARY_ID = 3L;
    private static final String VALID_PHOTO_KEY = "diary-photos/1/photo.jpg";

    // ── create ───────────────────────────────────────────────────────

    @Test
    void 일기_생성_사진없이_성공() {
        User user = mock(User.class);
        Festival festival = mock(Festival.class);
        given(festival.getId()).willReturn(FESTIVAL_ID);
        given(festival.getTitle()).willReturn("페스티벌명");

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(festivalRepository.findById(FESTIVAL_ID)).willReturn(Optional.of(festival));

        CreateDiaryRequestDto req = new CreateDiaryRequestDto(FESTIVAL_ID, "즐거운 하루였다", DiaryVisibility.PRIVATE, List.of());

        FestivalDiaryResponseDto result = diaryService.create(USER_ID, FESTIVAL_ID, req);

        then(diaryRepository).should().save(any(FestivalDiary.class));
        then(photoRepository).should(never()).save(any());
        assertThat(result.festivalId()).isEqualTo(FESTIVAL_ID);
        assertThat(result.content()).isEqualTo("즐거운 하루였다");
        assertThat(result.visibility()).isEqualTo(DiaryVisibility.PRIVATE);
        assertThat(result.photoUrls()).isEmpty();
    }

    @Test
    void 일기_생성_사진있으면_S3검증후_저장() {
        User user = mock(User.class);
        Festival festival = mock(Festival.class);
        given(festival.getId()).willReturn(FESTIVAL_ID);
        given(festival.getTitle()).willReturn("페스티벌명");

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(festivalRepository.findById(FESTIVAL_ID)).willReturn(Optional.of(festival));
        given(s3PresignService.presignGetUrl(anyString())).willReturn("https://s3.example.com/photo.jpg");

        CreateDiaryRequestDto req = new CreateDiaryRequestDto(
                FESTIVAL_ID, "즐거운 하루였다", DiaryVisibility.PUBLIC, List.of(VALID_PHOTO_KEY));

        FestivalDiaryResponseDto result = diaryService.create(USER_ID, FESTIVAL_ID, req);

        then(s3ObjectVerificationService).should().verifyImageObject(VALID_PHOTO_KEY);
        then(photoRepository).should(times(1)).save(any(FestivalDiaryPhoto.class));
        assertThat(result.photoUrls()).containsExactly("https://s3.example.com/photo.jpg");
    }

    @Test
    void 일기_생성_잘못된_사진키_예외() {
        User user = mock(User.class);
        Festival festival = mock(Festival.class);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(festivalRepository.findById(FESTIVAL_ID)).willReturn(Optional.of(festival));

        CreateDiaryRequestDto req = new CreateDiaryRequestDto(
                FESTIVAL_ID, "내용", DiaryVisibility.PRIVATE, List.of("wrong/key.jpg"));

        assertThatThrownBy(() -> diaryService.create(USER_ID, FESTIVAL_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 오브젝트 키입니다.");
        then(diaryRepository).should(never()).save(any());
    }

    // ── getMyDiaries ─────────────────────────────────────────────────

    @Test
    void 내_일기_목록_특정페스티벌만_조회() {
        FestivalDiary diary = mock(FestivalDiary.class);
        given(diary.getId()).willReturn(DIARY_ID);
        given(diaryRepository.findByUserIdAndFestivalIdOrderByCreatedAtDesc(USER_ID, FESTIVAL_ID))
                .willReturn(List.of(diary));
        given(photoRepository.findByDiaryIdIn(List.of(DIARY_ID))).willReturn(List.of());

        List<FestivalDiaryResponseDto> result = diaryService.getMyDiaries(USER_ID, FESTIVAL_ID);

        assertThat(result).hasSize(1);
        then(diaryRepository).should(never()).findByUserIdOrderByCreatedAtDesc(any());
    }

    @Test
    void 내_일기_목록_전체_조회() {
        given(diaryRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).willReturn(List.of());

        List<FestivalDiaryResponseDto> result = diaryService.getMyDiaries(USER_ID, null);

        assertThat(result).isEmpty();
    }

    // ── getDiary ─────────────────────────────────────────────────────

    @Test
    void 비공개_일기는_타인이_조회하면_예외() {
        FestivalDiary diary = mock(FestivalDiary.class);
        given(diary.getUserId()).willReturn(USER_ID);
        given(diary.isPublic()).willReturn(false);
        given(diaryRepository.findById(DIARY_ID)).willReturn(Optional.of(diary));

        assertThatThrownBy(() -> diaryService.getDiary(OTHER_USER_ID, DIARY_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("비공개 일기입니다.");
    }

    @Test
    void 공개_일기는_타인도_조회가능() {
        FestivalDiary diary = mock(FestivalDiary.class);
        given(diary.getUserId()).willReturn(USER_ID);
        given(diary.isPublic()).willReturn(true);
        given(diary.getUserNickname()).willReturn("작성자닉네임");
        given(diaryRepository.findById(DIARY_ID)).willReturn(Optional.of(diary));
        given(photoRepository.findByDiaryIdOrderBySortOrder(DIARY_ID)).willReturn(List.of());

        FestivalDiaryResponseDto result = diaryService.getDiary(OTHER_USER_ID, DIARY_ID);

        assertThat(result.isOwner()).isFalse();
        assertThat(result.authorNickname()).isEqualTo("작성자닉네임");
    }

    @Test
    void 본인_일기_조회시_작성자닉네임_null() {
        FestivalDiary diary = mock(FestivalDiary.class);
        given(diary.getUserId()).willReturn(USER_ID);
        given(diary.isPublic()).willReturn(false);
        given(diaryRepository.findById(DIARY_ID)).willReturn(Optional.of(diary));
        given(photoRepository.findByDiaryIdOrderBySortOrder(DIARY_ID)).willReturn(List.of());

        FestivalDiaryResponseDto result = diaryService.getDiary(USER_ID, DIARY_ID);

        assertThat(result.isOwner()).isTrue();
        assertThat(result.authorNickname()).isNull();
    }

    // ── update / delete ──────────────────────────────────────────────

    @Test
    void 일기_수정_소유자아니면_예외() {
        FestivalDiary diary = mock(FestivalDiary.class);
        given(diary.getUserId()).willReturn(USER_ID);
        given(diaryRepository.findById(DIARY_ID)).willReturn(Optional.of(diary));

        UpdateDiaryRequestDto req = new UpdateDiaryRequestDto("수정된 내용", DiaryVisibility.PUBLIC);

        assertThatThrownBy(() -> diaryService.update(OTHER_USER_ID, DIARY_ID, req))
                .isInstanceOf(AccessDeniedException.class);
        then(diary).should(never()).update(any(), any());
    }

    @Test
    void 일기_수정_성공() {
        FestivalDiary diary = mock(FestivalDiary.class);
        given(diary.getUserId()).willReturn(USER_ID);
        given(diaryRepository.findById(DIARY_ID)).willReturn(Optional.of(diary));
        given(photoRepository.findByDiaryIdOrderBySortOrder(DIARY_ID)).willReturn(List.of());

        UpdateDiaryRequestDto req = new UpdateDiaryRequestDto("수정된 내용", DiaryVisibility.PUBLIC);
        diaryService.update(USER_ID, DIARY_ID, req);

        then(diary).should().update("수정된 내용", DiaryVisibility.PUBLIC);
    }

    @Test
    void 일기_삭제시_사진_S3정리후_DB삭제() {
        FestivalDiary diary = mock(FestivalDiary.class);
        FestivalDiaryPhoto photo = mock(FestivalDiaryPhoto.class);
        given(diary.getUserId()).willReturn(USER_ID);
        given(photo.getPhotoKey()).willReturn(VALID_PHOTO_KEY);
        given(diaryRepository.findById(DIARY_ID)).willReturn(Optional.of(diary));
        given(photoRepository.findByDiaryIdOrderBySortOrder(DIARY_ID)).willReturn(List.of(photo));

        diaryService.delete(USER_ID, DIARY_ID);

        then(fileStorageService).should().deleteFileAfterCommit(VALID_PHOTO_KEY);
        then(diaryRepository).should().delete(diary);
    }

    @Test
    void 일기_삭제_소유자아니면_예외() {
        FestivalDiary diary = mock(FestivalDiary.class);
        given(diary.getUserId()).willReturn(USER_ID);
        given(diaryRepository.findById(DIARY_ID)).willReturn(Optional.of(diary));

        assertThatThrownBy(() -> diaryService.delete(OTHER_USER_ID, DIARY_ID))
                .isInstanceOf(AccessDeniedException.class);
        then(diaryRepository).should(never()).delete(any());
    }

    // ── getPublicFeed ────────────────────────────────────────────────

    @Test
    void 공개_피드는_차단유저_제외() {
        FestivalDiary diary = mock(FestivalDiary.class);
        Page<FestivalDiary> page = new PageImpl<>(List.of(diary), PageRequest.of(0, 10), 1);
        given(diaryRepository.findByFestivalIdAndVisibilityOrderByCreatedAtDesc(FESTIVAL_ID, DiaryVisibility.PUBLIC, PageRequest.of(0, 10)))
                .willReturn(page);
        given(blockedContentFilter.excludeBlocked(any(), any(), any())).willReturn(List.of());

        Page<FestivalDiaryResponseDto> result = diaryService.getPublicFeed(FESTIVAL_ID, 0, USER_ID);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── removeAllByUser ──────────────────────────────────────────────

    @Test
    void 탈퇴시_일기사진_S3정리후_DB삭제() {
        FestivalDiary diary = mock(FestivalDiary.class);
        FestivalDiaryPhoto photo = mock(FestivalDiaryPhoto.class);
        given(diary.getId()).willReturn(DIARY_ID);
        given(photo.getPhotoKey()).willReturn(VALID_PHOTO_KEY);
        given(diaryRepository.findByUserId(USER_ID)).willReturn(List.of(diary));
        given(photoRepository.findByDiaryIdIn(List.of(DIARY_ID))).willReturn(List.of(photo));

        diaryService.removeAllByUser(USER_ID);

        then(fileStorageService).should().deleteFileAfterCommit(VALID_PHOTO_KEY);
        then(diaryRepository).should().deleteByUserId(USER_ID);
    }

    @Test
    void 탈퇴시_일기없으면_사진조회_생략() {
        given(diaryRepository.findByUserId(USER_ID)).willReturn(List.of());

        diaryService.removeAllByUser(USER_ID);

        then(photoRepository).should(never()).findByDiaryIdIn(any());
        then(diaryRepository).should().deleteByUserId(USER_ID);
    }
}
