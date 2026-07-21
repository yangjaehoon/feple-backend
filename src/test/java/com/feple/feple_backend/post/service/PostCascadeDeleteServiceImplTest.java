package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PostCascadeDeleteServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock PostScrapRepository postScrapRepository;
    @Mock PostReportRepository postReportRepository;
    @Mock NotificationQueryService notificationQueryService;
    @Mock CommentService commentService;
    @Mock FileStorageService fileStorageService;

    @InjectMocks PostCascadeDeleteServiceImpl service;

    @Test
    void 아티스트_삭제시_게시글_이미지도_S3에서_정리() {
        Artist artist = mock(Artist.class);
        given(artist.getId()).willReturn(1L);

        Post postWithImage = Post.builder().id(10L).imageUrl("posts/a.jpg").build();
        Post postWithoutImage = Post.builder().id(11L).build();
        given(postRepository.findByArtist(artist)).willReturn(List.of(postWithImage, postWithoutImage));

        service.deletePostsByArtist(artist);

        then(fileStorageService).should().deleteFileAfterCommit("posts/a.jpg");
        then(postRepository).should().deleteAllByIdInBatch(List.of(10L, 11L));
    }

    @Test
    void 삭제할_게시글이_없으면_S3_정리도_생략() {
        Artist artist = mock(Artist.class);
        given(postRepository.findByArtist(artist)).willReturn(List.of());

        service.deletePostsByArtist(artist);

        then(fileStorageService).should(never()).deleteFileAfterCommit(anyString());
        then(postRepository).should(never()).deleteAllByIdInBatch(anyList());
    }
}
