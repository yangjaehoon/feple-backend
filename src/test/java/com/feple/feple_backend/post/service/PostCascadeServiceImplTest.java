package com.feple.feple_backend.post.service;

import static org.mockito.Mockito.verify;

import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import com.feple.feple_backend.post.repository.PostDraftRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostCascadeServiceImplTest {

    @Mock PostLikeRepository postLikeRepository;
    @Mock PostScrapRepository postScrapRepository;
    @Mock PostDraftRepository postDraftRepository;
    @Mock PostReportRepository postReportRepository;
    @Mock PostRepository postRepository;
    @Mock PostDeleter postDeleter;
    @Mock CommentService commentService;
    @Mock NotificationQueryService notificationQueryService;

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

    @Test
    void 회원_완전삭제시_임시저장과_이_유저가_낸_게시글신고_삭제() {
        postCascadeService.removeAuthoredArtifactsByUser(1L);

        verify(postDraftRepository).deleteByUserId(1L);
        verify(postReportRepository).deleteByReporterId(1L);
    }
}
