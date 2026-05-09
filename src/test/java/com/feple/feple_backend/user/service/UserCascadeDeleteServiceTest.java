package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.festival.entity.FestivalLike;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserDeviceToken;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserCascadeDeleteServiceTest {

    @Mock UserRepository userRepository;
    @Mock PostService postService;
    @Mock CommentRepository commentRepository;
    @Mock FestivalLikeRepository festivalLikeRepository;
    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock UserDeviceTokenRepository userDeviceTokenRepository;
    @Mock FestivalCertificationRepository certificationRepository;
    @Mock ArtistProfileImageLikeRepository artistImageLikeRepository;
    @Mock ArtistProfileImageRepository artistImageRepository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks UserCascadeDeleteService userCascadeDeleteService;

    private User userWithImage(Long id) {
        return User.builder()
                .id(id).oauthId("o" + id).nickname("user" + id)
                .profileImageUrl("profile/user-" + id + ".jpg")
                .build();
    }

    private void stubEmptyRelations(Long userId, User user) {
        given(commentRepository.findByUser(user)).willReturn(List.of());
        given(festivalLikeRepository.findByUserId(userId)).willReturn(List.of());
        given(artistFollowRepository.findByUserId(userId)).willReturn(List.of());
        given(userDeviceTokenRepository.findByUserId(userId)).willReturn(List.of());
    }

    @Test
    void 사용자_삭제시_댓글과_게시글_삭제됨() {
        User user = userWithImage(1L);
        List<Comment> comments = List.of(mock(Comment.class));
        given(commentRepository.findByUser(user)).willReturn(comments);
        given(festivalLikeRepository.findByUserId(1L)).willReturn(List.of());
        given(artistFollowRepository.findByUserId(1L)).willReturn(List.of());
        given(userDeviceTokenRepository.findByUserId(1L)).willReturn(List.of());

        userCascadeDeleteService.delete(user);

        verify(commentRepository).deleteAll(comments);
        verify(postService).deletePostsByUser(user);
    }

    @Test
    void 사용자_삭제시_팔로우_페스티벌좋아요_알림_삭제됨() {
        User user = userWithImage(1L);
        List<FestivalLike> festivalLikes = List.of(mock(FestivalLike.class));
        List<ArtistFollow> artistFollows = List.of(mock(ArtistFollow.class));
        given(commentRepository.findByUser(user)).willReturn(List.of());
        given(festivalLikeRepository.findByUserId(1L)).willReturn(festivalLikes);
        given(artistFollowRepository.findByUserId(1L)).willReturn(artistFollows);
        given(userDeviceTokenRepository.findByUserId(1L)).willReturn(List.of());

        userCascadeDeleteService.delete(user);

        verify(festivalLikeRepository).deleteAll(festivalLikes);
        verify(artistFollowRepository).deleteAll(artistFollows);
        verify(notificationRepository).deleteByUserId(1L);
    }

    @Test
    void 사용자_삭제시_기기토큰_인증_이미지좋아요_업로더참조_정리됨() {
        User user = userWithImage(1L);
        List<UserDeviceToken> tokens = List.of(mock(UserDeviceToken.class));
        given(commentRepository.findByUser(user)).willReturn(List.of());
        given(festivalLikeRepository.findByUserId(1L)).willReturn(List.of());
        given(artistFollowRepository.findByUserId(1L)).willReturn(List.of());
        given(userDeviceTokenRepository.findByUserId(1L)).willReturn(tokens);

        userCascadeDeleteService.delete(user);

        verify(userDeviceTokenRepository).deleteAll(tokens);
        verify(certificationRepository).deleteByUserId(1L);
        verify(artistImageLikeRepository).deleteByUserId(1L);
        verify(artistImageRepository).nullifyUploaderByUserId(1L);
    }

    @Test
    void 사용자_삭제시_유저_레코드와_프로필_이미지_파일_삭제됨() {
        User user = userWithImage(1L);
        stubEmptyRelations(1L, user);

        userCascadeDeleteService.delete(user);

        verify(userRepository).delete(user);
        verify(fileStorageService).deleteFile("profile/user-1.jpg");
    }

    @Test
    void 프로필_이미지_없는_사용자도_정상_삭제됨() {
        User user = User.builder().id(2L).oauthId("o2").nickname("noimage").build();
        stubEmptyRelations(2L, user);

        userCascadeDeleteService.delete(user);

        verify(userRepository).delete(user);
        verify(fileStorageService).deleteFile(null);
    }
}
