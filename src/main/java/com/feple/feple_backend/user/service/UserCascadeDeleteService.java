package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.service.ArtistGalleryPhotoService;
import com.feple.feple_backend.artist.song.service.SongRequestService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionService;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import com.feple.feple_backend.auth.service.RefreshTokenService;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.certification.service.FestivalReviewService;
import com.feple.feple_backend.comment.service.CommentReportService;
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
import com.feple.feple_backend.user.repository.UserPointLogRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.UserBlockService;
import com.feple.feple_backend.userreport.service.UserReportCleanupService;
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
    private final UserPointLogRepository userPointLogRepository;
    private final UserReportCleanupService userReportCleanupService;
    private final CommentReportService commentReportService;

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
     * 회원을 DB에서 물리적으로 제거한다(되돌릴 수 없음). 이 유저가 작성한 글·댓글과 거기 딸린
     * 다른 유저의 댓글·좋아요·신고까지 함께 삭제한다. 갤러리 사진을 올린 계정은 호출 측
     * (UserAdminServiceImpl)이 먼저 거부한다.
     */
    public void hardDelete(User user) {
        Long id = user.getId();
        String profileImageKey = user.getProfileImageUrl();

        removeAllActivity(id);
        purgeAuthoredContent(id);
        removeResidualUserReferences(id);

        userRepository.deleteById(id);

        fileStorageService.deleteFileAfterCommit(profileImageKey);
    }

    // 이 유저가 작성한 글·댓글을 물리 삭제 (일반 탈퇴는 익명화로 보존하지만 완전 삭제는 남길 수 없다).
    private void purgeAuthoredContent(Long id) {
        commentService.purgeAuthoredCommentsByUser(id);
        postCascadeService.purgeAuthoredPostsByUser(id);
    }

    // 일반 탈퇴(익명화)는 안 건드리지만 users 행 물리 삭제 전엔 비워야 하는 잔여 참조 (전부 users FK RESTRICT).
    // user 애그리거트 소유 테이블은 이 클래스가 직접, 다른 도메인은 해당 도메인 서비스로 위임한다.
    private void removeResidualUserReferences(Long id) {
        userAccessLogRepository.deleteByUserId(id);
        userPointLogRepository.deleteByUserId(id);
        userReportCleanupService.removeAllInvolvingUser(id);    // user_report(reporter_id·target_id)
        commentService.clearMentionsByUser(id);                 // comment.mentioned_user_id
        postCascadeService.removeAuthoredArtifactsByUser(id);   // post_draft + post_report(reporter)
        commentReportService.removeReportsByReporter(id);       // comment_report(reporter)
        artistGalleryPhotoService.removeReportsByReporter(id);  // artist_photo_report(reporter)
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
