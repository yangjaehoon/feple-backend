package com.feple.feple_backend.post.service;

import static org.mockito.Mockito.verify;

import com.feple.feple_backend.post.repository.PostDraftRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostCascadeDeleteServiceImplTest {

    @Mock PostLikeRepository postLikeRepository;
    @Mock PostScrapRepository postScrapRepository;
    @Mock PostDraftRepository postDraftRepository;
    @Mock PostReportRepository postReportRepository;

    private PostCascadeDeleteServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new PostCascadeDeleteServiceImpl(
                        postLikeRepository, postScrapRepository, postDraftRepository, postReportRepository);
    }

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
}
