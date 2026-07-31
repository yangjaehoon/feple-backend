package com.feple.feple_backend.post.service;

import static org.mockito.Mockito.verify;

import com.feple.feple_backend.post.repository.PostLikeRepository;
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
}
