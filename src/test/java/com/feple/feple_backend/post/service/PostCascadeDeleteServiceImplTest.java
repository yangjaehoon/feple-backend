package com.feple.feple_backend.post.service;

import static org.mockito.Mockito.verify;

import com.feple.feple_backend.post.repository.PostLikeRepository;
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

    private PostCascadeDeleteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PostCascadeDeleteServiceImpl(postLikeRepository, postScrapRepository);
    }

    @Test
    void 회원탈퇴시_좋아요_스크랩_카운트_감소후_삭제() {
        service.removePostActivityByUser(10L);

        verify(postLikeRepository).decrementPostLikeCountByUserId(10L);
        verify(postLikeRepository).deleteByUserId(10L);
        verify(postScrapRepository).decrementPostScrapCountByUserId(10L);
        verify(postScrapRepository).deleteByUserId(10L);
    }
}
