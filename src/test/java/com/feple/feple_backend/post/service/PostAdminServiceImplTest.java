package com.feple.feple_backend.post.service;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PostAdminServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock FileStorageService fileStorageService;

    PostAdminServiceImpl postAdminService;

    @BeforeEach
    void setUp() {
        postAdminService = new PostAdminServiceImpl(postRepository, eventPublisher,
                List.of(new ArtistPostFilterStrategy(postRepository), new FestivalPostFilterStrategy(postRepository)),
                fileStorageService);
    }

    // ── deletePost ───────────────────────────────────────────────────

    @Test
    void 관리자_게시글_삭제시_삭제이벤트_발행() {
        User author = user(1L);
        Post post = freePost(10L, author);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postAdminService.deletePost(10L);

        verify(eventPublisher).publishEvent(any(PostDeletedByAdminEvent.class));
        verify(postRepository).delete(any(com.feple.feple_backend.post.entity.Post.class));
    }

    // ── getPostForAdmin ────────────────────────────────────────────────

    @Test
    void 관리자_게시글_단건_조회시_작성자_프로필_이미지는_fileStorageService로_해소된_URL() {
        User author = user(1L);
        Post post = freePost(10L, author);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(fileStorageService.resolveProfileImageUrl(any())).willReturn("https://cdn.example.com/resolved.jpg");

        PostResponseDto result = postAdminService.getPostForAdmin(10L);

        assertThat(result.getProfileImageUrl()).isEqualTo("https://cdn.example.com/resolved.jpg");
    }

    // ── bulkDeletePosts ──────────────────────────────────────────────

    @Test
    void 게시글_일괄_삭제_성공() {
        postAdminService.bulkDeletePosts(List.of(1L, 2L, 3L));

        verify(postRepository).softDeleteByIds(List.of(1L, 2L, 3L));
    }

    @Test
    void 빈_ID_목록으로_일괄_삭제시_아무동작_안함() {
        postAdminService.bulkDeletePosts(List.of());

        verify(postRepository, never()).softDeleteByIds(any());
    }

    // ── restorePost / getDeletedPosts ──────────────────────────────────

    @Test
    void 게시글_복구_성공() {
        postAdminService.restorePost(10L);

        verify(postRepository).restore(10L);
    }

    @Test
    void 블라인드_해제_성공() {
        Post post = freePost(10L, user(1L));
        post.blind();
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postAdminService.unblindPost(10L);

        assertThat(post.isBlinded()).isFalse();
    }

    @Test
    void 삭제된_게시글_목록_조회() {
        User author = user(1L);
        given(postRepository.findSoftDeleted(20)).willReturn(List.of(freePost(1L, author)));

        List<PostResponseDto> result = postAdminService.getDeletedPosts(20);

        assertThat(result).hasSize(1);
    }

    // ── 모더레이션 요약 카운트 ──────────────────────────────────────────

    @Test
    void 유저_블라인드_게시글_수를_레포지토리에_위임() {
        given(postRepository.countBlindedByUserId(7L)).willReturn(2L);

        assertThat(postAdminService.countBlindedPostsByUser(7L)).isEqualTo(2L);
    }

    // ── getBlindedPosts ──────────────────────────────────────────────

    @Test
    void 블라인드된_게시글_목록_조회() {
        User author = user(1L);
        given(postRepository.findBlinded(20)).willReturn(List.of(freePost(1L, author)));

        List<PostResponseDto> result = postAdminService.getBlindedPosts(20);

        assertThat(result).hasSize(1);
    }

    // ── countPostsContaining / getPostCountsByUserIds ───────────────────

    @Test
    void 특정_단어_포함_게시글_수_조회() {
        given(postRepository.countByTitleOrContentContaining("공지")).willReturn(3L);

        assertThat(postAdminService.countPostsContaining("공지")).isEqualTo(3L);
    }

    @Test
    void 사용자별_게시글_수_조회() {
        given(postRepository.countGroupByUserId(List.of(1L, 2L)))
                .willReturn(List.of(new Object[]{1L, 5L}, new Object[]{2L, 3L}));

        Map<Long, Long> result = postAdminService.getPostCountsByUserIds(List.of(1L, 2L));

        assertThat(result).containsEntry(1L, 5L).containsEntry(2L, 3L);
    }

    @Test
    void 빈_사용자ID_목록으로_게시글_수_조회시_빈_맵_반환() {
        Map<Long, Long> result = postAdminService.getPostCountsByUserIds(List.of());

        assertThat(result).isEmpty();
        verify(postRepository, never()).countGroupByUserId(any());
    }

    // ── getPostsForAdmin ─────────────────────────────────────────────

    @Test
    void 관리자_게시판타입_필터로_게시글_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, "FREE", null, null, null);
        given(postRepository.findByBoardTypeOrderByCreatedAtDesc(eq(BoardType.FREE), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postAdminService.getPostsForAdmin(params);

        verify(postRepository).findByBoardTypeOrderByCreatedAtDesc(eq(BoardType.FREE), any(Pageable.class));
    }

    @Test
    void 관리자_아티스트ID_지정_필터로_게시글_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, "ARTIST", null, 3L, null);
        given(postRepository.findByArtistIdOrderByCreatedAtDesc(eq(3L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postAdminService.getPostsForAdmin(params);

        verify(postRepository).findByArtistIdOrderByCreatedAtDesc(eq(3L), any(Pageable.class));
    }

    @Test
    void 관리자_아티스트_필터_ID없이_전체_아티스트_게시글_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, "ARTIST", null, null, null);
        given(postRepository.findByArtistIsNotNullOrderByCreatedAtDesc(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postAdminService.getPostsForAdmin(params);

        verify(postRepository).findByArtistIsNotNullOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void 관리자_필터_없으면_전체_게시글_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, null, null, null, null);
        given(postRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postAdminService.getPostsForAdmin(params);

        verify(postRepository).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void 관리자_키워드_포함시_제목_검색으로_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, null, "공지", null, null);
        given(postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postAdminService.getPostsForAdmin(params);

        verify(postRepository).findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(anyString(), any(Pageable.class));
    }

    @Test
    void 관리자_게시판타입과_키워드_함께_지정시_해당_조합으로_조회() {
        PostAdminFilterDto params = new PostAdminFilterDto(0, 10, "FREE", "공지", null, null);
        given(postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                        eq(BoardType.FREE), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        postAdminService.getPostsForAdmin(params);

        verify(postRepository).findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                eq(BoardType.FREE), anyString(), any(Pageable.class));
    }

    // ── getTotalPostCount / countRecentPosts / getAdminHotPosts ────────

    @Test
    void 전체_게시글_수_조회() {
        given(postRepository.count()).willReturn(100L);

        assertThat(postAdminService.getTotalPostCount()).isEqualTo(100L);
    }

    @Test
    void 최근_게시글_수_조회() {
        given(postRepository.countByCreatedAtAfter(any())).willReturn(7L);

        assertThat(postAdminService.countRecentPosts(7)).isEqualTo(7L);
    }

    @Test
    void 관리자_인기_게시글_목록_조회() {
        User author = user(1L);
        given(postRepository.findPopularPosts(any(), any(Pageable.class)))
                .willReturn(List.of(freePost(1L, author)));

        List<PostResponseDto> result = postAdminService.getAdminHotPosts(4);

        assertThat(result).hasSize(1);
    }

    // ── getRecentPostsByUser ─────────────────────────────────────────

    @Test
    void 사용자_최근_게시글_목록_조회() {
        User author = user(1L);
        given(postRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postAdminService.getRecentPostsByUser(1L, 5);

        assertThat(result).hasSize(1);
    }

    // ── togglePin ────────────────────────────────────────────────────

    @Test
    void 게시글_고정_토글시_상태가_반전되고_반전된_값을_반환() {
        User author = user(1L);
        Post post = freePost(10L, author);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        boolean pinned = postAdminService.togglePin(10L);

        assertThat(pinned).isTrue();
        assertThat(post.isPinned()).isTrue();
    }

    @Test
    void 이미_고정된_게시글_토글시_고정_해제() {
        User author = user(1L);
        Post post = freePost(10L, author);
        post.togglePinned();
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        boolean pinned = postAdminService.togglePin(10L);

        assertThat(pinned).isFalse();
    }
}
