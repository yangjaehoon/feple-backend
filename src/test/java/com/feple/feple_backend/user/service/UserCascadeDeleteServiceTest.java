package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.service.ArtistGalleryPhotoService;
import com.feple.feple_backend.artist.photo.service.ArtistProfileImageLikeService;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.artist.suggestion.repository.ArtistSuggestionRepository;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import com.feple.feple_backend.auth.repository.RefreshTokenRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.festival.service.FestivalAttendanceService;
import com.feple.feple_backend.festival.service.FestivalLikeService;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.repository.NotificationPreferenceRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.post.service.PostCascadeService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
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

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock UserDeviceTokenRepository userDeviceTokenRepository;

    @Mock FestivalLikeService festivalLikeService;
    @Mock FestivalAttendanceService festivalAttendanceService;
    @Mock ArtistFollowService artistFollowService;
    @Mock PostCascadeService postCascadeService;
    @Mock CommentService commentService;
    @Mock ArtistGalleryPhotoService artistGalleryPhotoService;
    @Mock ArtistProfileImageLikeService artistProfileImageLikeService;

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationPreferenceRepository notificationPreferenceRepository;
    @Mock FestivalCertificationRepository certificationRepository;
    @Mock FestivalCertificationService certificationService;
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

        verify(festivalLikeService).removeAllByUser(1L);
        verify(artistFollowService).removeAllByUser(1L);
        verify(notificationRepository).deleteByUserId(1L);
        verify(notificationPreferenceRepository).deleteByUserId(1L);
    }

    @Test
    void 사용자_삭제시_기기토큰_인증_이미지좋아요_업로더참조_정리됨() {
        User user = userWithImage(1L);

        userCascadeDeleteService.delete(user);

        verify(userDeviceTokenRepository).deleteByUserId(1L);
        verify(certificationService).removeReviewLikesByUser(1L);
        verify(certificationRepository).deleteByUserId(1L);
        verify(songRequestRepository).deleteByUserId(1L);
        verify(artistSuggestionRepository).deleteByUserId(1L);
        verify(artistProfileImageLikeService).removeByUser(1L);
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

        // 게시글/댓글 행 삭제 없이 소프트딜리트(익명화)만 수행
        verify(postCascadeService).removePostActivityByUser(1L);
        verify(commentService).removeLikesByUser(1L);
    }

    @Test
    void 사용자_삭제시_카운터_감소_포함_서비스_위임_호출됨() {
        User user = userWithImage(1L);

        userCascadeDeleteService.delete(user);

        verify(festivalLikeService).removeAllByUser(1L);
        verify(festivalAttendanceService).removeAllByUser(1L);
        verify(artistFollowService).removeAllByUser(1L);
        verify(postCascadeService).removePostActivityByUser(1L);
        verify(commentService).removeLikesByUser(1L);
        verify(artistProfileImageLikeService).removeByUser(1L);
        verify(artistGalleryPhotoService).removeByUser(1L);
    }

    @Test
    void 프로필_이미지_없는_사용자도_정상_삭제됨() {
        User user = User.builder().id(2L).oauthId("o2").nickname("noimage").build();

        userCascadeDeleteService.delete(user);

        verify(fileStorageService).deleteFileAfterCommit(null);
        assertThat(user.isDeleted()).isTrue();
    }
}
