package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.artist.suggestion.repository.ArtistSuggestionRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.auth.repository.RefreshTokenRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.comment.repository.CommentLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalAttendanceRepository;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.repository.NotificationPreferenceRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCascadeDeleteServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock FestivalLikeRepository festivalLikeRepository;
    @Mock FestivalAttendanceRepository festivalAttendanceRepository;
    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock UserDeviceTokenRepository userDeviceTokenRepository;
    @Mock FestivalCertificationRepository certificationRepository;
    @Mock ArtistProfileImageLikeRepository artistImageLikeRepository;
    @Mock ArtistProfileImageRepository artistImageRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock CommentLikeRepository commentLikeRepository;
    @Mock PostScrapRepository postScrapRepository;
    @Mock SongRequestRepository songRequestRepository;
    @Mock ArtistSuggestionRepository artistSuggestionRepository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks UserCascadeDeleteService userCascadeDeleteService;

    private User userWithImage(Long id) {
        return User.builder()
                .id(id).oauthId("o" + id).nickname("user" + id)
                .profileImageUrl("profile/user-" + id + ".jpg")
                .build();
    }

    @Test
    void 사용자_삭제시_팔로우_페스티벌좋아요_알림_삭제됨() {
        User user = userWithImage(1L);

        userCascadeDeleteService.delete(user);

        verify(festivalLikeRepository).deleteByUserId(1L);
        verify(artistFollowRepository).deleteByUserId(1L);
        verify(notificationRepository).deleteByUserId(1L);
        verify(notificationPreferenceRepository).deleteByUserId(1L);
    }

    @Test
    void 사용자_삭제시_기기토큰_인증_이미지좋아요_업로더참조_정리됨() {
        User user = userWithImage(1L);

        userCascadeDeleteService.delete(user);

        verify(userDeviceTokenRepository).deleteByUserId(1L);
        verify(certificationRepository).deleteByUserId(1L);
        verify(songRequestRepository).deleteByUserId(1L);
        verify(artistSuggestionRepository).deleteByUserId(1L);
        verify(artistImageLikeRepository).deleteByUserId(1L);
        verify(artistImageRepository).nullifyUploaderByUserId(1L);
    }

    @Test
    void 사용자_삭제시_리프레시토큰_무효화_및_소프트삭제됨() {
        User user = userWithImage(1L);

        userCascadeDeleteService.delete(user);

        verify(refreshTokenRepository).deleteByUserId(1L);
        verify(fileStorageService).deleteFileAfterCommit("profile/user-1.jpg");
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getNickname()).isEqualTo("(탈퇴한 사용자)");
    }

    @Test
    void 게시글과_댓글은_익명화_유지하며_삭제하지_않음() {
        User user = userWithImage(1L);

        userCascadeDeleteService.delete(user);

        verify(userRepository, never()).delete(user);
    }

    @Test
    void 사용자_삭제시_카운터_감소_후_행_삭제됨() {
        User user = userWithImage(1L);
        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                festivalLikeRepository, festivalAttendanceRepository,
                artistFollowRepository, postLikeRepository,
                commentLikeRepository, postScrapRepository);

        userCascadeDeleteService.delete(user);

        inOrder.verify(festivalLikeRepository).decrementFestivalLikeCountByUserId(1L);
        inOrder.verify(festivalLikeRepository).deleteByUserId(1L);
        inOrder.verify(festivalAttendanceRepository).decrementAttendingCountByUserId(1L);
        inOrder.verify(festivalAttendanceRepository).deleteByUserId(1L);
        inOrder.verify(artistFollowRepository).decrementFollowerCountByUserId(1L);
        inOrder.verify(artistFollowRepository).deleteByUserId(1L);
        inOrder.verify(postLikeRepository).decrementPostLikeCountByUserId(1L);
        inOrder.verify(postLikeRepository).deleteByUserId(1L);
        inOrder.verify(commentLikeRepository).decrementCommentLikeCountByUserId(1L);
        inOrder.verify(commentLikeRepository).deleteByUserId(1L);
        inOrder.verify(postScrapRepository).decrementPostScrapCountByUserId(1L);
        inOrder.verify(postScrapRepository).deleteByUserId(1L);
    }

    @Test
    void 프로필_이미지_없는_사용자도_정상_삭제됨() {
        User user = User.builder().id(2L).oauthId("o2").nickname("noimage").build();

        userCascadeDeleteService.delete(user);

        verify(fileStorageService).deleteFileAfterCommit(null);
        assertThat(user.isDeleted()).isTrue();
    }
}
