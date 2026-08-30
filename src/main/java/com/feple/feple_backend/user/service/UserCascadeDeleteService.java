package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.service.ArtistGalleryPhotoService;
import com.feple.feple_backend.artist.song.service.SongRequestService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionService;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import com.feple.feple_backend.auth.service.RefreshTokenService;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.certification.service.FestivalReviewService;
import com.feple.feple_backend.comment.repository.CommentRepository;
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
import com.feple.feple_backend.user.repository.UserAccessLogRepository;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.UserBlockService;
import com.feple.feple_backend.userreport.repository.UserReportRepository;
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

    private final UserAccessLogRepository userAccessLogRepository;
    private final UserReportRepository userReportRepository;
    private final CommentRepository commentRepository;

    public void delete(User user, WithdrawalReason reason, String detail) {
        String profileImageKey = user.getProfileImageUrl();

        removeAllActivity(user.getId());

        // 게시글·댓글은 익명 처리 후 유지 (작성자 닉네임은 "(탈퇴한 사용자)"로 표시됨)
        // removeAllActivity의 카운터 감소용 @Modifying(clearAutomatically = true) 쿼리가 영속성
        // 컨텍스트를 비워 user가 detached 상태가 된다 — dirty checking에 의존하지 않고 save()로
        // 명시적으로 병합·flush해야 softDelete()가 실제로 반영된다.
        user.softDelete(reason, detail);
        userRepository.save(user);

        fileStorageService.deleteFileAfterCommit(profileImageKey);
    }

    /**
     * 이미 탈퇴 처리된 회원을 DB에서 물리적으로 제거한다(되돌릴 수 없음).
     * 호출 측(UserAdminServiceImpl)이 "탈퇴 상태 + 작성 글·댓글 없음"을 먼저 검증한다.
     * 게시글·댓글이 없는 계정(예: 로그인으로 재접근 불가능해진 OAuth 테스트 계정) 정리 전용이다.
     */
    public void hardDelete(User user) {
        Long id = user.getId();
        String profileImageKey = user.getProfileImageUrl();

        removeAllActivity(id);

        // 소프트 삭제 캐스케이드가 다루지 않는 잔여 참조 — 남으면 users 행 DELETE가 FK로 실패한다.
        userAccessLogRepository.deleteByUserId(id);
        userReportRepository.deleteByUserInvolved(id);
        commentRepository.clearMentionsByUserId(id);

        userRepository.deleteById(id);

        fileStorageService.deleteFileAfterCommit(profileImageKey);
    }

    // 소셜 활동·인증 세션 데이터 삭제 — 각 도메인 서비스에 위임해 카운터 정합성을 보장한다.
    private void removeAllActivity(Long id) {
        refreshTokenService.revokeAll(id);
        userDeviceTokenRepository.deleteByUserId(id);

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
    }
}
