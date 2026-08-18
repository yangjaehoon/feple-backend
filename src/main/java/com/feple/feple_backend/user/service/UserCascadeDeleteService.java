package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.service.ArtistGalleryPhotoService;
import com.feple.feple_backend.artist.song.service.SongRequestService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionService;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import com.feple.feple_backend.auth.service.RefreshTokenService;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.certification.service.FestivalReviewService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.diary.service.FestivalDiaryService;
import com.feple.feple_backend.festival.service.FestivalAttendanceService;
import com.feple.feple_backend.festival.service.FestivalLikeService;
import com.feple.feple_backend.festival.suggestion.service.FestivalSuggestionService;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.service.NotificationPreferenceService;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import com.feple.feple_backend.post.service.PostCascadeDeleteService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.WithdrawalReason;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class UserCascadeDeleteService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserDeviceTokenRepository userDeviceTokenRepository;

    private final FestivalLikeService festivalLikeService;
    private final FestivalAttendanceService festivalAttendanceService;
    private final ArtistFollowService artistFollowService;
    private final PostCascadeDeleteService postCascadeService;
    private final CommentService commentService;
    private final ArtistGalleryPhotoService artistGalleryPhotoService;

    private final NotificationQueryService notificationQueryService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final FestivalCertificationService certificationService;
    private final FestivalReviewService reviewService;
    private final FestivalDiaryService festivalDiaryService;
    private final SongRequestService songRequestService;
    private final ArtistSuggestionService artistSuggestionService;
    private final FestivalSuggestionService festivalSuggestionService;

    private final UserBlockService userBlockService;
    private final FileStorageService fileStorageService;

    public void delete(User user, WithdrawalReason reason, String detail) {
        Long id = user.getId();
        String profileImageKey = user.getProfileImageUrl();

        // 인증 세션 무효화
        refreshTokenService.revokeAll(id);
        userDeviceTokenRepository.deleteByUserId(id);

        // 소셜 활동 데이터 삭제 — 각 도메인 서비스에 위임하여 카운터 정합성 보장
        festivalLikeService.removeAllByUser(id);
        festivalAttendanceService.removeAllByUser(id);
        artistFollowService.removeAllByUser(id);
        postCascadeService.removePostActivityByUser(id);
        commentService.removeLikesByUser(id);
        artistGalleryPhotoService.removeByUser(id);

        notificationQueryService.deleteAll(id);
        notificationPreferenceService.removeAllByUser(id);
        reviewService.removeReviewLikesByUser(id);
        certificationService.removeAllByUser(id);
        festivalDiaryService.removeAllByUser(id);
        songRequestService.removeAllByUser(id);
        artistSuggestionService.removeAllByUser(id);
        festivalSuggestionService.removeAllByUser(id);
        userBlockService.removeAllByUser(id);

        // 게시글·댓글은 익명 처리 후 유지 (작성자 닉네임은 "(탈퇴한 사용자)"로 표시됨)
        // 위의 각 removeAllByUser 호출이 카운터 감소용 @Modifying(clearAutomatically = true) 쿼리를
        // 실행하면서 영속성 컨텍스트를 비워 user가 detached 상태가 된다 — dirty checking에 의존하지 않고
        // save()로 명시적으로 병합·flush해야 softDelete()가 실제로 반영된다.
        user.softDelete(reason, detail);
        userRepository.save(user);

        fileStorageService.deleteFileAfterCommit(profileImageKey);
    }
}
