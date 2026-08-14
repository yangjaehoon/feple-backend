package com.feple.feple_backend.comment.service;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.comment.dto.CommentLikeResult;
import com.feple.feple_backend.comment.dto.CommentResponseDto;
import com.feple.feple_backend.comment.dto.CreateCommentDto;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.comment.event.CommentCreatedEvent;
import com.feple.feple_backend.comment.repository.CommentLikeRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.exception.BadWordException;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import com.feple.feple_backend.userblock.service.UserBlockService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock CommentRepository commentRepository;
    @Mock CommentDeleter commentDeleter;
    @Mock CommentLikeRepository commentLikeRepository;
    @Mock PostRepository postRepository;
    @Mock PostService postService;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock FestivalCertificationService certificationService;
    @Mock BadWordValidator badWordValidator;
    @Mock UserBlockService userBlockService;
    @Spy BlockedContentFilter blockedContentFilter = new BlockedContentFilter(mock(UserBlockService.class));

    @Mock FileStorageService fileStorageService;
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
    void 댓글_생성시_작성자_프로필_이미지는_fileStorageService로_해소된_URL() {
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
        given(fileStorageService.resolveProfileImageUrl(any())).willReturn("https://cdn.example.com/resolved.jpg");

        CommentResponseDto result = commentService.createComment(dto, 2L);

        assertThat(result.getProfileImageUrl()).isEqualTo("https://cdn.example.com/resolved.jpg");
    }

    // postService.incrementCommentCount()는 @Modifying(clearAutomatically = true)라 호출 직후
    // 영속성 컨텍스트를 통째로 비운다. DTO 변환보다 먼저 호출되면 saved의 지연 로딩 연관관계
    // (mentionedUser 등)에 접근할 때 LazyInitializationException이 난다 — DTO 변환 이후에
    // 호출되는 순서를 고정한다.
    @Test
    void 댓글_생성시_댓글수_증가는_DTO_변환_이후에_호출된다() {
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

        commentService.createComment(dto, 2L);

        InOrder order = inOrder(commentRepository, fileStorageService, postService);
        order.verify(commentRepository).save(any(Comment.class));
        order.verify(fileStorageService).resolveProfileImageUrl(any());
        order.verify(postService).incrementCommentCount(10L);
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

        // 알림 대상은 없지만(postAuthorId/mentionedUserId null), 포인트 지급을 위해 이벤트는 발행됨
        ArgumentCaptor<CommentCreatedEvent> captor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().postAuthorId()).isNull();
        assertThat(captor.getValue().mentionedUserId()).isNull();
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
    void 대댓글의_대댓글은_최상위_댓글로_평탄화() {
        User postAuthor = user(1L);
        User replier = user(2L);
        User grandReplier = user(3L);
        Post post = freePost(10L, postAuthor);
        Comment root = comment(50L, post, postAuthor);
        Comment reply = Comment.builder()
                .id(60L).content("답글").post(post).user(replier).parent(root)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        CreateCommentDto dto = mock(CreateCommentDto.class);
        given(dto.getPostId()).willReturn(10L);
        given(dto.getContent()).willReturn("답글의 답글");
        given(dto.getParentId()).willReturn(60L);

        Comment saved = comment(100L, post, grandReplier);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(3L)).willReturn(Optional.of(grandReplier));
        given(commentRepository.findById(60L)).willReturn(Optional.of(reply));
        given(commentRepository.save(any(Comment.class))).willReturn(saved);

        commentService.createComment(dto, 3L);

        // 저장되는 parentId는 최상위 댓글(50L)로 평탄화되지만, 멘션 대상은 실제로 답글을 단
        // reply(작성자 2L)를 가리켜야 한다 — 평탄화된 parent(postAuthor=1L)가 아님.
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getParentId()).isEqualTo(50L);
        assertThat(captor.getValue().getMentionedUserId()).isEqualTo(2L);

        ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().mentionedUserId()).isEqualTo(2L);
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

    @Test
    void 대댓글_작성자가_게시글작성자_대댓글작성자_모두_아니면_알림대상에_포함() {
        User postAuthor = user(1L);
        User parentAuthor = user(3L);
        User commenter = user(2L);
        Post post = freePost(10L, postAuthor);
        Comment parent = comment(50L, post, parentAuthor);

        CreateCommentDto dto = mock(CreateCommentDto.class);
        given(dto.getPostId()).willReturn(10L);
        given(dto.getContent()).willReturn("대댓글내용");
        given(dto.getParentId()).willReturn(50L);

        Comment saved = comment(100L, post, commenter);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(2L)).willReturn(Optional.of(commenter));
        given(commentRepository.findById(50L)).willReturn(Optional.of(parent));
        given(commentRepository.save(any(Comment.class))).willReturn(saved);

        commentService.createComment(dto, 2L);

        ArgumentCaptor<CommentCreatedEvent> captor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().mentionedUserId()).isEqualTo(3L);
    }

    // ── getCommentsByPost ────────────────────────────────────────────

    @Test
    void 댓글_없는_게시글_빈_목록_반환() {
        User author = user(1L);
        Post post = freePost(10L, author);

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(eq(10L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        List<CommentResponseDto> result = commentService.getCommentsByPost(10L, null, null);

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

        List<CommentResponseDto> result = commentService.getCommentsByPost(10L, 1L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }

    @Test
    void 베스트순_정렬시_최상위_댓글은_좋아요순_답글은_항상_작성순() {
        User author = user(1L);
        Post post = freePost(10L, author);
        LocalDateTime t0 = LocalDateTime.now();
        Comment rootLowLike = Comment.builder()
                .id(100L).content("낮은 좋아요").post(post).user(author)
                .likeCount(1).createdAt(t0).updatedAt(t0).build();
        Comment rootHighLike = Comment.builder()
                .id(101L).content("높은 좋아요").post(post).user(author)
                .likeCount(10).createdAt(t0.plusMinutes(1)).updatedAt(t0.plusMinutes(1)).build();
        Comment replyToLowLike = Comment.builder()
                .id(102L).content("답글").post(post).user(author).parent(rootLowLike)
                .likeCount(0).createdAt(t0.plusMinutes(2)).updatedAt(t0.plusMinutes(2)).build();

        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(commentRepository.findByPostIdOrderByCreatedAtAsc(eq(10L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(rootLowLike, rootHighLike, replyToLowLike)));
        given(commentLikeRepository.findLikedCommentIdsByUserAndCommentIds(eq(1L), any()))
                .willReturn(List.of());

        List<CommentResponseDto> result = commentService.getCommentsByPost(10L, 1L, "best");

        // 좋아요 높은 rootHighLike가 먼저, 좋아요 낮은 rootLowLike는 그 답글(replyToLowLike)과 묶여 뒤에 온다
        assertThat(result).extracting(CommentResponseDto::getId)
                .containsExactly(101L, 100L, 102L);
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

        List<CommentResponseDto> result = commentService.getCommentsByPost(10L, 1L, null);

        assertThat(result.get(0).isCertified()).isTrue();
    }

    // ── deleteOwnComment ─────────────────────────────────────────────

    @Test
    void 본인_댓글_삭제_성공() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(commentRepository.findByIdIgnoringRestrictions(100L)).willReturn(Optional.of(c));

        commentService.deleteOwnComment(100L, 1L);

        verify(commentRepository).softDeleteById(100L);
        verify(postService).decrementCommentCount(10L);
    }

    @Test
    void 타인이_댓글_삭제시_접근_거부_예외() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(commentRepository.findByIdIgnoringRestrictions(100L)).willReturn(Optional.of(c));

        assertThatThrownBy(() -> commentService.deleteOwnComment(100L, 2L))
                .isInstanceOf(AccessDeniedException.class);

        verify(commentRepository, never()).softDeleteById(any());
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

    // ── getAdminCommentsByPost ───────────────────────────────────────

    @Test
    void 관리자용_댓글_목록_조회() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(commentRepository.findByPostIdIgnoringBlindOrderByCreatedAtAsc(10L, 50))
                .willReturn(List.of(c));

        List<CommentResponseDto> result = commentService.getAdminCommentsByPost(10L, 50);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isCertified()).isFalse();
    }

    // ── getMyComments ─────────────────────────────────────────────────

    @Test
    void 내_댓글_목록_조회() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(commentRepository.findByUserOrderByCreatedAtDesc(eq(author), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(c)));

        List<MyCommentResponseDto> result = commentService.getMyComments(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCommentId()).isEqualTo(100L);
    }

    @Test
    void 존재하지_않는_사용자의_내_댓글_조회시_예외() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getMyComments(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── getRecentCommentsByUser ──────────────────────────────────────

    @Test
    void 최근_댓글_목록_조회() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(commentRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(c)));

        List<MyCommentResponseDto> result = commentService.getRecentCommentsByUser(1L, 5);

        assertThat(result).hasSize(1);
    }

    // ── countMyComments ───────────────────────────────────────────────

    @Test
    void 내_댓글_수_조회() {
        given(commentRepository.countByUserId(1L)).willReturn(7L);

        long count = commentService.countMyComments(1L);

        assertThat(count).isEqualTo(7L);
    }

    // ── deleteComment (관리자) ────────────────────────────────────────

    @Test
    void 관리자_댓글_삭제() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(commentRepository.findByIdIgnoringRestrictions(100L)).willReturn(Optional.of(c));

        commentService.deleteComment(100L);

        verify(commentRepository).softDeleteById(100L);
        verify(postService).decrementCommentCount(10L);
    }

    @Test
    void 존재하지_않는_댓글_관리자_삭제시_예외() {
        given(commentRepository.findByIdIgnoringRestrictions(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(999L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── countCommentsContaining ──────────────────────────────────────

    @Test
    void 특정_단어_포함_댓글수_조회() {
        given(commentRepository.countByContentContaining("바보")).willReturn(3L);

        long count = commentService.countCommentsContaining("바보");

        assertThat(count).isEqualTo(3L);
        verify(commentRepository).countByContentContaining("바보");
    }

    // ── getCommentCountsByUserIds ────────────────────────────────────

    @Test
    void 유저ID_비어있으면_빈맵_반환() {
        Map<Long, Long> result = commentService.getCommentCountsByUserIds(List.of());

        assertThat(result).isEmpty();
        verify(commentRepository, never()).countGroupByUserId(any());
    }

    @Test
    void 유저별_댓글수_집계() {
        given(commentRepository.countGroupByUserId(List.of(1L)))
                .willReturn(List.<Object[]>of(new Object[]{1L, 4L}));

        Map<Long, Long> result = commentService.getCommentCountsByUserIds(List.of(1L));

        assertThat(result).containsEntry(1L, 4L);
    }

    // ── updateOwnComment ──────────────────────────────────────────────

    @Test
    void 본인_댓글_수정_성공() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(commentRepository.findByIdIgnoringRestrictions(100L)).willReturn(Optional.of(c));

        commentService.updateOwnComment(100L, 1L, "수정된 내용");

        assertThat(c.getContent()).isEqualTo("수정된 내용");
        verify(badWordValidator).validateField("content", "수정된 내용");
    }

    @Test
    void 타인이_댓글_수정시_접근_거부_예외() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(commentRepository.findByIdIgnoringRestrictions(100L)).willReturn(Optional.of(c));

        assertThatThrownBy(() -> commentService.updateOwnComment(100L, 2L, "수정 시도"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void 금칙어_포함_댓글_수정시_예외() {
        User author = user(1L);
        Post post = freePost(10L, author);
        Comment c = comment(100L, post, author);

        given(commentRepository.findByIdIgnoringRestrictions(100L)).willReturn(Optional.of(c));
        willThrow(new BadWordException("content"))
                .given(badWordValidator).validateField(eq("content"), anyString());

        assertThatThrownBy(() -> commentService.updateOwnComment(100L, 1L, "욕설포함"))
                .isInstanceOf(BadWordException.class);
    }

    // ── deleteByPostIds ───────────────────────────────────────────────

    @Test
    void 게시글ID목록으로_댓글_일괄삭제시_commentDeleter에_위임() {
        commentService.deleteByPostIds(List.of(10L, 20L));

        verify(commentDeleter).deleteByPostIds(List.of(10L, 20L));
    }

    // ── removeLikesByUser ─────────────────────────────────────────────

    @Test
    void 사용자_좋아요_전체_삭제시_카운트감소_및_삭제() {
        commentService.removeLikesByUser(1L);

        verify(commentLikeRepository).decrementCommentLikeCountByUserId(1L);
        verify(commentLikeRepository).deleteByUserId(1L);
    }
}
