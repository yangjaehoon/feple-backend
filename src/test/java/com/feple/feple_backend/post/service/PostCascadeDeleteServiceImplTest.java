package com.feple.feple_backend.post.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import com.feple.feple_backend.post.repository.PostDraftRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostCascadeDeleteServiceImplTest {

    @Mock PostLikeRepository postLikeRepository;
    @Mock PostScrapRepository postScrapRepository;
    @Mock PostDraftRepository postDraftRepository;
    @Mock PostReportRepository postReportRepository;
    @Mock PostRepository postRepository;
    @Mock PostDeleter postDeleter;
    @Mock CommentService commentService;
    @Mock NotificationQueryService notificationQueryService;

    @InjectMocks PostCascadeDeleteServiceImpl service;

    @Test
    void 회원탈퇴시_좋아요_스크랩_카운트_감소후_삭제() {
        service.removePostActivityByUser(10L);

        verify(postLikeRepository).decrementPostLikeCountByUserId(10L);
        verify(postLikeRepository).deleteByUserId(10L);
        verify(postScrapRepository).decrementPostScrapCountByUserId(10L);
        verify(postScrapRepository).deleteByUserId(10L);
    }

    @Test
    void 회원_완전삭제시_임시저장과_이_유저가_낸_게시글신고_삭제() {
        service.removeAuthoredArtifactsByUser(10L);

        verify(postDraftRepository).deleteByUserId(10L);
        verify(postReportRepository).deleteByReporterId(10L);
    }

    @Test
    void purgeAuthoredPostsByUser_작성글이_없으면_아무것도_안함() {
        given(postRepository.findIdsByUserId(10L)).willReturn(List.of());

        service.purgeAuthoredPostsByUser(10L);

        verify(commentService, never()).deleteByPostIds(org.mockito.ArgumentMatchers.anyList());
        verify(postDeleter, never()).deleteByIds(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void purgeAuthoredPostsByUser_작성글의_댓글_알림_자식_순서로_물리삭제() {
        List<Long> postIds = List.of(1L, 2L, 3L);
        given(postRepository.findIdsByUserId(10L)).willReturn(postIds);

        service.purgeAuthoredPostsByUser(10L);

        verify(commentService).deleteByPostIds(postIds);
        verify(notificationQueryService).deleteByPostIds(postIds);
        verify(postDeleter).deleteByIds(postIds);
    }
}
