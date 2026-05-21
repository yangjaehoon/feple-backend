package com.feple.feple_backend.post.service;

import com.feple.feple_backend.badword.BadWordFilter;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock UserRepository userRepository;
    @Mock ArtistRepository artistRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock FestivalCertificationRepository certificationRepository;
    @Mock BadWordFilter badWordFilter;

    @InjectMocks PostServiceImpl postService;

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
    }

    private Post freePost(Long id, User author) {
        return Post.builder()
                .id(id).title("제목" + id).content("내용")
                .user(author).boardType(BoardType.FREE)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ── createPost ──────────────────────────────────────────────────

    @Test
    void 게시글_생성_성공() {
        User author = user(1L);
        PostRequestDto dto = PostRequestDto.builder().title("제목").content("내용")
                .boardType(BoardType.FREE).build();
        Post saved = freePost(10L, author);

        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(saved);

        Long id = postService.createPost(dto, 1L);

        assertThat(id).isEqualTo(10L);
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void 존재하지_않는_사용자로_게시글_생성시_예외() {
        PostRequestDto dto = PostRequestDto.builder().title("t").content("c")
                .boardType(BoardType.FREE).build();
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(dto, 99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── getPost ──────────────────────────────────────────────────────

    @Test
    void 게시글_단건_조회_성공() {
        User author = user(1L);
        Post post = freePost(10L, author);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        PostResponseDto result = postService.getPost(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getNickname()).isEqualTo("user1");
    }

    @Test
    void 존재하지_않는_게시글_조회시_예외() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── deleteOwnPost ────────────────────────────────────────────────

    @Test
    void 본인_게시글_삭제_성공() {
        User author = user(1L);
        Post post = freePost(10L, author);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postService.deleteOwnPost(10L, 1L);

        verify(postLikeRepository).deleteByPostId(10L);
        verify(postRepository).deleteById(10L);
    }

    @Test
    void 타인이_게시글_삭제시_접근_거부_예외() {
        User owner = user(1L);
        Post post = freePost(10L, owner);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deleteOwnPost(10L, 2L))
                .isInstanceOf(AccessDeniedException.class);

        verify(postRepository, never()).deleteById(any());
    }

    // ── getPostsByFestivalId ──────────────────────────────────────────

    @Test
    void 페스티벌_게시글_인증된_사용자_플래그_true() {
        User certifiedUser = user(1L);
        Festival festival = Festival.builder().id(5L).title("락 페스티벌").build();
        Post post = Post.builder()
                .id(10L).title("후기").content("좋았음")
                .user(certifiedUser).festival(festival)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(certificationRepository.findApprovedUserIdsByFestivalId(5L)).willReturn(Set.of(1L));
        given(postRepository.findByFestivalOrderByCreatedAtDesc(eq(festival), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post)));

        List<PostResponseDto> result = postService.getPostsByFestivalId(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isCertified()).isTrue();
    }

    @Test
    void 페스티벌_게시글_미인증_사용자_플래그_false() {
        User uncertified = user(2L);
        Festival festival = Festival.builder().id(5L).title("락 페스티벌").build();
        Post post = Post.builder()
                .id(11L).title("일반 후기").content("내용")
                .user(uncertified).festival(festival)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(certificationRepository.findApprovedUserIdsByFestivalId(5L)).willReturn(Set.of(1L));
        given(postRepository.findByFestivalOrderByCreatedAtDesc(eq(festival), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post)));

        List<PostResponseDto> result = postService.getPostsByFestivalId(5L);

        assertThat(result.get(0).isCertified()).isFalse();
    }

    // ── getHotPosts ──────────────────────────────────────────────────

    @Test
    void 핫_게시글_최대_4개_반환() {
        User author = user(1L);
        List<Post> hotPosts = List.of(
                freePost(1L, author), freePost(2L, author),
                freePost(3L, author), freePost(4L, author));
        given(postRepository.findHotPosts(any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(hotPosts);

        List<PostResponseDto> result = postService.getHotPosts();

        assertThat(result).hasSize(4);
    }

    @Test
    void 핫_게시글_없으면_빈_리스트() {
        given(postRepository.findHotPosts(any(LocalDateTime.class), any(Pageable.class)))
                .willReturn(List.of());

        assertThat(postService.getHotPosts()).isEmpty();
    }

    // ── getPostsByArtistId ────────────────────────────────────────────

    @Test
    void 아티스트_게시글_목록_조회() {
        User author = user(1L);
        Artist artist = Artist.builder().id(3L).name("아이유").build();
        Post post = Post.builder()
                .id(20L).title("아이유 게시글").content("내용")
                .user(author).artist(artist)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        given(artistRepository.findById(3L)).willReturn(Optional.of(artist));
        given(postRepository.findByArtistOrderByCreatedAtDesc(eq(artist), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(post)));

        List<PostResponseDto> result = postService.getPostsByArtistId(3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBoardDisplayName()).isEqualTo("아이유 게시판");
    }
}
