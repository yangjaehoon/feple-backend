package com.feple.feple_backend.post.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.repository.PostImageRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import com.feple.feple_backend.post.repository.PostTagRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostDeleterTest {

    @Mock PostImageRepository postImageRepository;
    @Mock PostTagRepository postTagRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock PostScrapRepository postScrapRepository;
    @Mock PostReportRepository postReportRepository;
    @Mock PostRepository postRepository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks PostDeleter postDeleter;

    @Test
    void 빈목록이면_아무것도_안함() {
        postDeleter.deleteByIds(List.of());

        then(postRepository).shouldHaveNoInteractions();
        then(postLikeRepository).shouldHaveNoInteractions();
        then(fileStorageService).shouldHaveNoInteractions();
    }

    @Test
    void S3_이미지_정리_예약_후_자식_행_먼저_그다음_게시글_물리삭제() {
        List<Long> ids = List.of(1L, 2L);
        given(postImageRepository.findImageKeysByPostIds(ids)).willReturn(List.of("k1", "k2"));

        postDeleter.deleteByIds(ids);

        verify(fileStorageService).deleteFileAfterCommit("k1");
        verify(fileStorageService).deleteFileAfterCommit("k2");

        InOrder order = inOrder(postImageRepository, postTagRepository, postLikeRepository,
                postScrapRepository, postReportRepository, postRepository);
        order.verify(postImageRepository).deleteByPostIds(ids);
        order.verify(postTagRepository).deleteByPostIds(ids);
        order.verify(postLikeRepository).deleteByPostIds(ids);
        order.verify(postScrapRepository).deleteByPostIds(ids);
        order.verify(postReportRepository).deleteByPostIds(ids);
        order.verify(postRepository).hardDeleteByIds(ids);
    }
}
