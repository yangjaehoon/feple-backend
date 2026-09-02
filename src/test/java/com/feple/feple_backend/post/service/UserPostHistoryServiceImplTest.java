package com.feple.feple_backend.post.service;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class UserPostHistoryServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock UserRepository userRepository;

    @Mock FileStorageService fileStorageService;
    @InjectMocks UserPostHistoryServiceImpl service;

    @Test
    void getMyPosts_유저_게시글_최신순_조회() {
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findByUserOrderByCreatedAtDesc(eq(author), any()))
                .willReturn(new PageImpl<>(List.of(freePost(10L, author))));

        List<PostResponseDto> result = service.getMyPosts(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getMyPosts_작성자_프로필_이미지는_fileStorageService로_해소된_URL() {
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findByUserOrderByCreatedAtDesc(eq(author), any()))
                .willReturn(new PageImpl<>(List.of(freePost(10L, author))));
        given(fileStorageService.resolveProfileImageUrl(any())).willReturn("https://cdn.example.com/resolved.jpg");

        List<PostResponseDto> result = service.getMyPosts(1L);

        assertThat(result.get(0).getProfileImageUrl()).isEqualTo("https://cdn.example.com/resolved.jpg");
    }

    @Test
    void getMyPosts_존재하지_않는_사용자면_예외() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyPosts(99L)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getMyPostsPaged_커서없으면_처음부터_조회() {
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findByUserOrderByIdDesc(eq(author), any()))
                .willReturn(List.of(freePost(10L, author)));

        CursorPage<PostResponseDto> result = service.getMyPostsPaged(1L, null, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void getMyPostsPaged_커서있으면_이전ID_기준_조회() {
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findByUserAndIdLessThanOrderByIdDesc(eq(author), eq(10L), any()))
                .willReturn(List.of(freePost(5L, author)));

        CursorPage<PostResponseDto> result = service.getMyPostsPaged(1L, 10L, 20);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void getMyPostsPaged_다음페이지_있으면_hasNext_true_다음커서_설정() {
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findByUserOrderByIdDesc(eq(author), any()))
                .willReturn(List.of(freePost(10L, author), freePost(9L, author)));

        CursorPage<PostResponseDto> result = service.getMyPostsPaged(1L, null, 1);

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(10L);
    }

    @Test
    void getPublicPostsPaged_커서없으면_공개글만_조회() {
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findPublicByUserOrderByIdDesc(eq(author), any()))
                .willReturn(List.of(freePost(10L, author)));

        CursorPage<PostResponseDto> result = service.getPublicPostsPaged(1L, null, 20);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void getPublicPostsPaged_커서있으면_이전ID_기준_공개글_조회() {
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findPublicByUserAndIdLessThanOrderByIdDesc(eq(author), eq(10L), any()))
                .willReturn(List.of(freePost(5L, author)));

        CursorPage<PostResponseDto> result = service.getPublicPostsPaged(1L, 10L, 20);

        assertThat(result.content()).hasSize(1);
    }

    @Test
    void countPublicPosts_레포지토리에_위임() {
        given(postRepository.countPublicByUserId(1L)).willReturn(5L);

        assertThat(service.countPublicPosts(1L)).isEqualTo(5L);
    }

    @Test
    void countVisiblePosts_익명_포함_카운트를_레포지토리에_위임() {
        given(postRepository.countByUserId(1L)).willReturn(8L);

        assertThat(service.countVisiblePosts(1L)).isEqualTo(8L);
    }

    @Test
    void getLikedPosts_좋아요한_게시글_조회() {
        User author = user(1L);
        given(postLikeRepository.findPostsByUserId(eq(1L), any()))
                .willReturn(List.of(freePost(10L, author)));

        List<PostResponseDto> result = service.getLikedPosts(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void countLikedPosts_레포지토리에_위임() {
        given(postLikeRepository.countByUserId(1L)).willReturn(3L);

        assertThat(service.countLikedPosts(1L)).isEqualTo(3L);
    }
}
