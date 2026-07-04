package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.repository.PostRepository;

import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostAdminServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks PostAdminServiceImpl postAdminService;

    // ── deletePost ───────────────────────────────────────────────────

    @Test
    void 관리자_게시글_삭제시_삭제이벤트_발행() {
        User author = user(1L);
        Post post = freePost(10L, author);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));

        postAdminService.deletePost(10L);

        verify(eventPublisher).publishEvent(any(PostDeletedByAdminEvent.class));
        verify(postRepository).deleteById(10L);
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
    void 삭제된_게시글_목록_조회() {
        User author = user(1L);
        given(postRepository.findSoftDeleted(20)).willReturn(List.of(freePost(1L, author)));

        List<PostResponseDto> result = postAdminService.getDeletedPosts(20);

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
}
