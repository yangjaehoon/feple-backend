package com.feple.feple_backend.post.service;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostTag;
import com.feple.feple_backend.post.event.PostCreatedEvent;
import com.feple.feple_backend.post.repository.PostDraftRepository;
import com.feple.feple_backend.post.repository.PostImageRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostTagRepository;
import com.feple.feple_backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PostWriterTest {

    @Mock PostRepository postRepository;
    @Mock PostImageRepository postImageRepository;
    @Mock PostTagRepository postTagRepository;
    @Mock PostDraftRepository postDraftRepository;
    @Mock FileStorageService fileStorageService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks PostWriter postWriter;

    private static PostContext freeBoard() {
        return new PostContext(BoardType.FREE, null, null);
    }

    @Test
    void 저장시_임시저장_삭제하고_생성_이벤트를_발행() {
        User author = user(1L);
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용")
                .build();
        given(postRepository.save(any(Post.class))).willReturn(freePost(10L, author));

        Long id = postWriter.save(dto, author, freeBoard());

        assertThat(id).isEqualTo(10L);
        verify(postDraftRepository).deleteByUserId(1L);
        verify(eventPublisher).publishEvent(any(PostCreatedEvent.class));
    }

    @Test
    void 태그는_정규화되고_중복이_제거되어_저장된다() {
        User author = user(1L);
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용")
                .tags(List.of("#Rock", " rock ", "festival")).build();
        given(postRepository.save(any(Post.class))).willReturn(freePost(10L, author));

        postWriter.save(dto, author, freeBoard());

        ArgumentCaptor<List<PostTag>> captor = ArgumentCaptor.forClass(List.class);
        verify(postTagRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(PostTag::getTag).containsExactly("rock", "festival");
    }

    @Test
    void 수정시_제목_내용을_반영하고_이미지_태그를_재저장() {
        User author = user(1L);
        Post post = freePost(10L, author);
        PostRequestDto dto = PostRequestDto.builder().title("수정된 제목").content("수정된 내용")
                .build();
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postWriter.update(10L, dto);

        assertThat(post.getTitle()).isEqualTo("수정된 제목");
        assertThat(post.getContent()).isEqualTo("수정된 내용");
        verify(postImageRepository).deleteByPostId(10L);
        verify(postTagRepository).deleteByPostId(10L);
    }

    @Test
    void 수정으로_빠진_이미지의_S3_객체만_정리하고_유지된_이미지는_건드리지_않는다() {
        // 행만 지우고 객체를 남기면 사진을 줄일 때마다 S3에 고아가 쌓이고,
        // presigned GET URL을 아는 사람은 계속 접근할 수 있다.
        User author = user(1L);
        Post post = freePost(10L, author);
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용")
                .imageUrls(List.of("posts/keep.jpg"))
                .build();
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(postImageRepository.findImageKeysByPostIds(List.of(10L)))
                .willReturn(List.of("posts/keep.jpg", "posts/removed1.jpg", "posts/removed2.jpg"));

        postWriter.update(10L, dto);

        verify(fileStorageService).deleteFileAfterCommit("posts/removed1.jpg");
        verify(fileStorageService).deleteFileAfterCommit("posts/removed2.jpg");
        verify(fileStorageService, never()).deleteFileAfterCommit("posts/keep.jpg");
    }
}
