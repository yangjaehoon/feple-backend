package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.global.exception.BadWordException;
import com.feple.feple_backend.comment.dto.CommentLikeResult;
import com.feple.feple_backend.comment.dto.CommentResponseDto;
import com.feple.feple_backend.comment.dto.CreateCommentDto;
import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.comment.event.CommentCreatedEvent;
import com.feple.feple_backend.comment.repository.CommentLikeRepository;
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import com.feple.feple_backend.userblock.service.UserBlockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock CommentRepository commentRepository;
    @Mock CommentReportRepository commentReportRepository;
    @Mock CommentLikeRepository commentLikeRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock FestivalCertificationService certificationService;
    @Mock BadWordValidator badWordValidator;
    @Mock UserBlockService userBlockService;
    @Spy BlockedContentFilter blockedContentFilter = new BlockedContentFilter(mock(UserBlockService.class));

    @InjectMocks CommentServiceImpl commentService;

    private Comment comment(Long id, Post post, User author) {
        return Comment.builder()
                .id(id).content("댓글내용").post(post).user(author)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ── createComment ────────────────────────────────────────────────

    @Test
    void 댓글_생성_성공() {
        User postAuthor = user(1L);
        User commenter = user(2L);
        Post post = freePost(10L, postAuthor);

        CreateCommentDto dto = mock(CreateCommentDto.class);
        given(dto.getPostId()).willReturn(10L);
        given(dto.getContent()).willReturn("댓글내용");
        given(dto.getParentId()).willReturn(null);

        Comment saved = comment(100L, post, commenter);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(commenter));
        given(commentRepository.save(any(Comment.class))).willReturn(saved);

        CommentResponseDto result = commentService.createComment(dto, 2L);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getNickname()).isEqualTo("user2");
    }

    @Test
    void 게시글_작성자가_차단한_사용자는_댓글_작성_불가() {
        User postAuthor = user(1L);
        Post post = freePost(10L, postAuthor);

        CreateCommentDto dto = mock(CreateCommentDto.class);
        given(dto.getPostId()).willReturn(10L);
        given(dto.getContent()).willReturn("댓글내용");

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userBlockService.isBlocked(1L, 2L)).willReturn(true);

        assertThatThrownBy(() -> commentService.createComment(dto, 2L))
                .isInstanceOf(AccessDeniedException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void 금칙어_포함_댓글_생성시_예외() {
        CreateCommentDto dto = mock(CreateCommentDto.class);
        given(dto.getContent()).willReturn("욕설포함댓글");
        willThrow(new BadWordException("content"))
                .given(badWordValidator).validateField(eq("content"), anyString());

        assertThatThrownBy(() -> commentService.createComment(dto, 2L))
                .isInstanceOf(BadWordException.class);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void 댓글_생성시_게시글_작성자와_다른_사용자면_이벤트_발행() {
        User postAuthor = user(1L);
        User commenter = user(2L);
        Post post = freePost(10L, postAuthor);

        CreateCommentDto dto = mock(CreateCommentDto.class);
        given(dto.getPostId()).willReturn(10L);
        given(dto.getContent()).willReturn("댓글");
        given(dto.getParentId()).willReturn(null);

        Comment saved = comment(100L, post, commenter);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(commenter));
        given(commentRepository.save(any(Comment.class))).willReturn(saved);

        commentService.createComment(dto, 2L);

        verify(eventPublisher).publishEvent(any(CommentCreatedEvent.class));
    }

    @Test
    void 댓글_생성시_게시글_작성자_본인이면_알림대상없이_포인트용_이벤트_발행() {
        User author = user(1L);
        Post post = freePost(10L, author);

        CreateCommentDto dto = mock(CreateCommentDto.class);
        given(dto.getPostId()).willReturn(10L);
        given(dto.getContent()).willReturn("내 게시글에 셀프 댓글");
        given(dto.getParentId()).willReturn(null);

        Comment saved = comment(100L, post, author);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(commentRepository.save(any(Comment.class))).willReturn(saved);

        commentService.createComment(dto, 1L);

        // 알림 대상은 없지만(postAuthorId/parentCommentAuthorId null), 포인트 지급을 위해 이벤트는 발행됨
        ArgumentCaptor<CommentCreatedEvent> captor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().postAuthorId()).isNull();
        assertThat(captor.getValue().parentCommentAuthorId()).isNull();
        assertThat(captor.getValue().commenterId()).isEqualTo(1L);
    }

    @Test
    void 대댓글_생성_성공() {
        User postAuthor = user(1L);
        User commenter = user(2L);
        Post post = freePost(10L, postAuthor);
        Comment parent = comment(50L, post, postAuthor);

        CreateCommentDto dto = mock(CreateCommentDto.class);
        given(dto.getPostId()).willReturn(10L);
        given(dto.getContent()).willReturn("대댓글내용");
        given(dto.getParentId()).willReturn(50L);

        Comment saved = comment(100L, post, commenter);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(commenter));
        given(commentRepository.findById(50L)).willReturn(Optional.of(parent));
        given(commentRepository.save(any(Comment.class))).willReturn(saved);

        CommentResponseDto result = commentService.createComment(dto, 2L);

        assertThat(result.getId()).isEqualTo(100L);
        verify(commentRepository).findById(50L);
    }

    @Test
    void 다른_게시글의_댓글을_부모로_지정하면_예외() {
        User postAuthor = user(1L);
        User otherAuthor = user(3L);
        User commenter = user(2L);
        Post post = freePost(10L, postAuthor);
        Post otherPost = freePost(20L, otherAuthor);
        Comment parentOnOtherPost = comment(50L, otherPost, otherAuthor);

        CreateCommentDto dto = mock(CreateCommentDto.class);
        given(dto.getPostId()).willReturn(10L);
        given(dto.getContent()).willReturn("대댓글내용");
        given(dto.getParentId()).willReturn(50L);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(commenter));
        given(commentRepository.findById(50L)).willReturn(Optional.of(parentOnOtherPost));

        assertThatThrownBy(() -> commentService.createComment(dto, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 게시글에 속하지 않습니다");
        verify(commentRepository, never()).save(any());
    }

    @Test
    void 존재하지_않는_게시글에_댓글_생성시_예외() {
        CreateCommentDto dto = mock(CreateCommentDto.class);
        given(dto.getPostId()).willReturn(99L);
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(dto, 1L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── getCommentsByPost ────────────────────────────────────────────

    @Test
    void 댓글_없는_게시글_빈_목록_반환() {
        User author = user(1L);
        Post post = freePost(10L, author);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(eq(10L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        List<CommentResponseDto> result = commentService.getCommentsByPost(10L, null);

        assertThat(result).isEmpty();
    }

    @Test
    void 게시글_댓글_목록_조회() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(eq(10L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(c)));
        given(commentLikeRepository.findLikedCommentIdsByUserAndCommentIds(eq(1L), any()))
                .willReturn(List.of());

        List<CommentResponseDto> result = commentService.getCommentsByPost(10L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }

    @Test
    void 페스티벌_게시글_댓글_인증된_작성자_플래그_true() {
        User author = user(1L);
        Festival festival = Festival.builder().id(5L).title("락 페스티벌").build();
        Post post = Post.builder()
                .id(10L).title("후기").content("내용")
                .user(author).festival(festival)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        Comment c = comment(100L, post, author);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(certificationService.findApprovedUserIdsByFestivalId(5L)).willReturn(Set.of(1L));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(eq(10L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(c)));
        given(commentLikeRepository.findLikedCommentIdsByUserAndCommentIds(eq(1L), any()))
                .willReturn(List.of());

        List<CommentResponseDto> result = commentService.getCommentsByPost(10L, 1L);

        assertThat(result.get(0).isCertified()).isTrue();
    }

    // ── deleteOwnComment ─────────────────────────────────────────────

    @Test
    void 본인_댓글_삭제_성공() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(commentRepository.findById(100L)).willReturn(Optional.of(c));

        commentService.deleteOwnComment(100L, 1L);

        verify(commentRepository).deleteById(100L);
        verify(postRepository).decrementCommentCount(10L);
    }

    @Test
    void 타인이_댓글_삭제시_접근_거부_예외() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(commentRepository.findById(100L)).willReturn(Optional.of(c));

        assertThatThrownBy(() -> commentService.deleteOwnComment(100L, 2L))
                .isInstanceOf(AccessDeniedException.class);

        verify(commentRepository, never()).deleteById(any());
    }

    // ── toggleLike ───────────────────────────────────────────────────

    @Test
    void 댓글_좋아요_추가() {
        User liker = user(2L);
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment before = comment(100L, post, author);
        Comment after = Comment.builder()
                .id(100L).content("댓글내용").post(post).user(author)
                .likeCount(1)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        // 원자적 UPDATE 이후 재조회하는 값이 실제로 반영된 카운트인지 검증하기 위해
        // findById를 토글 전(before)/후(after) 값을 순서대로 반환하도록 스텁한다
        given(commentRepository.findById(100L)).willReturn(Optional.of(before), Optional.of(after));
        given(userRepository.findById(2L)).willReturn(Optional.of(liker));
        given(commentLikeRepository.deleteByUserIdAndCommentId(2L, 100L)).willReturn(0);

        CommentLikeResult result = commentService.toggleLike(100L, 2L);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1);
        verify(commentLikeRepository).saveAndFlush(any());
        verify(commentRepository).incrementLikeCount(100L);
    }

    @Test
    void 댓글_좋아요_취소() {
        User liker = user(2L);
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment before = Comment.builder()
                .id(100L).content("댓글내용").post(post).user(author)
                .likeCount(1)
                .createdAt(java.time.LocalDateTime.now()).updatedAt(java.time.LocalDateTime.now())
                .build();
        Comment after = Comment.builder()
                .id(100L).content("댓글내용").post(post).user(author)
                .likeCount(0)
                .createdAt(java.time.LocalDateTime.now()).updatedAt(java.time.LocalDateTime.now())
                .build();

        given(commentRepository.findById(100L)).willReturn(Optional.of(before), Optional.of(after));
        given(userRepository.findById(2L)).willReturn(Optional.of(liker));
        given(commentLikeRepository.deleteByUserIdAndCommentId(2L, 100L)).willReturn(1);

        CommentLikeResult result = commentService.toggleLike(100L, 2L);

        assertThat(result.liked()).isFalse();
        assertThat(result.likeCount()).isEqualTo(0);
        verify(commentLikeRepository).deleteByUserIdAndCommentId(2L, 100L);
        verify(commentRepository).decrementLikeCount(100L);
    }
}
