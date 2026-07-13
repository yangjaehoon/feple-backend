package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.repository.PostRepository;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import com.feple.feple_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostCascadeServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock PostScrapRepository postScrapRepository;
    @Mock PostReportRepository postReportRepository;
    @Mock NotificationQueryService notificationQueryService;
    @Mock CommentService commentService;

    @InjectMocks PostCascadeDeleteServiceImpl postCascadeService;

    // ── removePostActivityByUser ───────────────────────────────────────

    @Test
    void 사용자_게시글_활동_삭제시_좋아요와_스크랩_모두_정리() {
        postCascadeService.removePostActivityByUser(1L);

        verify(postLikeRepository).decrementPostLikeCountByUserId(1L);
        verify(postLikeRepository).deleteByUserId(1L);
        verify(postScrapRepository).decrementPostScrapCountByUserId(1L);
        verify(postScrapRepository).deleteByUserId(1L);
    }

    // ── deletePostsByArtist ──────────────────────────────────────────

    @Test
    void 아티스트_게시글_일괄_삭제시_연관데이터_모두_삭제() {
        User author = user(1L);
        Artist artist = Artist.builder().id(3L).name("아이유").build();
        Post post = Post.builder()
                .id(20L).title("t").content("c").user(author).artist(artist)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        given(postRepository.findByArtist(artist)).willReturn(List.of(post));

        postCascadeService.deletePostsByArtist(artist);

        verify(postRepository).nullifyArtistIdForSoftDeleted(3L);
        verify(commentService).deleteByPostIds(List.of(20L));
        verify(postLikeRepository).deleteByPostIds(List.of(20L));
        verify(postScrapRepository).deleteByPostIds(List.of(20L));
        verify(postReportRepository).deleteByPostIds(List.of(20L));
        verify(notificationQueryService).removeAllByPostIds(List.of(20L));
        verify(postRepository).deleteAllByIdInBatch(List.of(20L));
    }

    @Test
    void 아티스트에_연관된_게시글이_없으면_연관데이터_삭제_스킵() {
        Artist artist = Artist.builder().id(3L).name("아이유").build();
        given(postRepository.findByArtist(artist)).willReturn(List.of());

        postCascadeService.deletePostsByArtist(artist);

        verify(postRepository).nullifyArtistIdForSoftDeleted(3L);
        verify(commentService, never()).deleteByPostIds(any());
        verify(postRepository, never()).deleteAllByIdInBatch(any());
    }
}
