package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.comment.dto.CommentLikeResult;
import com.feple.feple_backend.comment.dto.CommentResponseDto;
import com.feple.feple_backend.comment.dto.CreateCommentDto;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.comment.entity.CommentLike;
import com.feple.feple_backend.comment.event.CommentCreatedEvent;
import com.feple.feple_backend.comment.repository.CommentLikeRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.LikeToggler;
import com.feple.feple_backend.global.OwnershipValidator;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import com.feple.feple_backend.userblock.service.UserBlockService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentDeleter commentDeleter;
    private final PostRepository postRepository;
    private final PostService postService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FestivalCertificationService certificationService;
    private final BadWordValidator badWordValidator;
    private final UserBlockService userBlockService;
    private final BlockedContentFilter blockedContentFilter;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public CommentResponseDto createComment(CreateCommentDto dto, Long userId) {
        badWordValidator.validateField("content", dto.getContent());
        Post post = EntityLoader.getOrThrow(postRepository::findById, dto.getPostId(), "게시글");

        if (userBlockService.isBlocked(post.getUserId(), userId)) {
            throw new AccessDeniedException("차단된 사용자의 게시글에는 댓글을 작성할 수 없습니다.");
        }

        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");

        ParentResolution parentResolution = resolveParent(dto.getParentId(), post);
        Comment saved = saveComment(dto, post, user, parentResolution);

        publishCommentCreatedEvent(dto, post, user, parentResolution, userId);

        boolean certified = post.getFestivalId() != null &&
                certificationService.existsApprovedCertification(post.getFestivalId(), userId);
        CommentResponseDto response = CommentResponseDto.from(saved, new CommentResponseDto.ViewerContext(certified, false), fileStorageService);

        postService.incrementCommentCount(post.getId());

        return response;
    }

    // storageParent: 실제 저장될 부모(depth 1단계로 평탄화된 최상위 댓글, 최상위 댓글이면 null).
    // mentionTarget: 사용자가 실제로 "답글"을 누른 댓글 — 평탄화 이후에도 멘션·알림 대상은 이걸 써야 한다.
    private record ParentResolution(Comment storageParent, Comment mentionTarget) {}

    // 답글에 다시 답글을 달면 depth가 계속 깊어져 대화가 읽기 어려워지므로, 답글의 답글은
    // 항상 최상위 댓글로 평탄화한다(depth 1단계 고정) — 다른 커뮤니티 앱들의 일반적인 동작과 동일.
    private ParentResolution resolveParent(Long parentId, Post post) {
        if (parentId == null) return new ParentResolution(null, null);
        Comment requestedParent = EntityLoader.getOrThrow(commentRepository::findById, parentId, "부모 댓글");
        if (!requestedParent.getPostId().equals(post.getId())) {
            throw new InvalidRequestException("부모 댓글이 해당 게시글에 속하지 않습니다.");
        }
        Comment storageParent = requestedParent.getParentId() != null ? requestedParent.getParent() : requestedParent;
        return new ParentResolution(storageParent, requestedParent);
    }

    private Comment saveComment(CreateCommentDto dto, Post post, User user, ParentResolution parentResolution) {
        User mentionedUser = parentResolution.mentionTarget() != null ? parentResolution.mentionTarget().getUser() : null;
        Comment comment = new Comment(dto.getContent(), post, user,
                parentResolution.storageParent(), mentionedUser, dto.isAnonymous());
        return commentRepository.save(comment);
    }

    private void publishCommentCreatedEvent(CreateCommentDto dto, Post post, User user,
                                             ParentResolution parentResolution, Long userId) {
        Long postAuthorId = post.getUserId();
        String commenterName = dto.isAnonymous() ? "익명" : user.getNickname();
        Long mentionedUserId = resolveMentionedUserId(parentResolution.mentionTarget(), userId, postAuthorId);
        // 게시글 작성자 본인이 자기 글에 댓글을 달면 게시글 알림은 생략 (원댓글 알림은 그대로 유지)
        Long notifyPostAuthorId = postAuthorId.equals(userId) ? null : postAuthorId;

        eventPublisher.publishEvent(
                new CommentCreatedEvent(notifyPostAuthorId, commenterName, post.getTitle(), post.getId(), mentionedUserId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByPost(Long postId, Long userId, String sort) {
        Post post = EntityLoader.getOrThrow(postRepository::findById, postId, "게시글");
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(0, PageSize.COMMENTS)).getContent();
        if ("best".equals(sort)) {
            comments = CommentSorter.sortByBest(comments);
        }
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        List<Long> authorIds = comments.stream().map(Comment::getUserId).distinct().toList();

        Set<Long> certifiedUserIds = getCertifiedUserIds(post, authorIds);
        Set<Long> likedCommentIds = getLikedCommentIds(userId, commentIds);

        List<CommentResponseDto> result = comments.stream()
                .map(c -> CommentResponseDto.from(
                        c,
                        new CommentResponseDto.ViewerContext(certifiedUserIds.contains(c.getUserId()), likedCommentIds.contains(c.getId())),
                        fileStorageService))
                .toList();
        return blockedContentFilter.excludeBlocked(result, userId, CommentResponseDto::getUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getAdminCommentsByPost(Long postId, int limit) {
        return commentRepository.findAdminByPostIdOrderByCreatedAtAsc(postId, limit).stream()
                .map(c -> CommentResponseDto.from(c, new CommentResponseDto.ViewerContext(false, false), fileStorageService))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyCommentResponseDto> getMyComments(Long userId) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        return commentRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, PageSize.MY_ACTIVITIES))
                .stream().map(MyCommentResponseDto::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyCommentResponseDto> getRecentCommentsByUser(Long userId, int limit) {
        return commentRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .stream().map(MyCommentResponseDto::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyComments(Long userId) {
        return commentRepository.countByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        // soft delete: 신고 기록(CommentReport) 보존, 행이 남아 FK 무결성 유지.
        deleteAndDecrement(EntityLoader.getOrThrow(commentRepository::findById, commentId, "댓글"));
    }

    @Override
    @Transactional
    public void deleteOwnComment(Long commentId, Long requestUserId) {
        Comment comment = EntityLoader.getOrThrow(commentRepository::findById, commentId, "댓글");
        OwnershipValidator.checkOwner(comment.getUserId(), requestUserId, "댓글");
        deleteAndDecrement(comment);
    }

    private Long resolveMentionedUserId(Comment mentionTarget, Long userId, Long postAuthorId) {
        if (mentionTarget == null) return null;
        Long mentionedUserId = mentionTarget.getUserId();
        if (mentionedUserId.equals(userId) || mentionedUserId.equals(postAuthorId)) return null;
        return mentionedUserId;
    }

    private void deleteAndDecrement(Comment comment) {
        // @SQLDelete가 걸려 있어 remove가 deleted_at 세팅(소프트 삭제)로 동작한다
        commentRepository.delete(comment);
        postService.decrementCommentCount(comment.getPostId());
    }

    @Override
    @Transactional(readOnly = true)
    public long countCommentsContaining(String word) {
        return commentRepository.countByContentContaining(word.toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getCommentCountsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return QueryResultMapper.toLongMap(commentRepository.countGroupByUserId(userIds));
    }

    @Override
    @Transactional
    public void updateOwnComment(Long commentId, Long requestUserId, String content) {
        Comment comment = EntityLoader.getOrThrow(commentRepository::findById, commentId, "댓글");
        OwnershipValidator.checkOwner(comment.getUserId(), requestUserId, "댓글", "수정");
        badWordValidator.validateField("content", content);
        comment.update(content);
    }

    // 인증 뱃지 확인은 이 목록에 등장하는 작성자로만 범위를 좁힌다 (페스티벌 전체 인증자 로드 방지)
    private Set<Long> getCertifiedUserIds(Post post, List<Long> authorIds) {
        if (post.getFestivalId() == null || authorIds.isEmpty()) return Set.of();
        return certificationService.findApprovedUserIdsByFestivalId(post.getFestivalId(), authorIds);
    }

    @Override
    @Transactional
    public void deleteByPostIds(List<Long> postIds) {
        commentDeleter.deleteByPostIds(postIds);
    }

    @Override
    @Transactional
    public void purgeAuthoredCommentsByUser(Long userId) {
        commentDeleter.deleteByAuthorId(userId);
    }

    private Set<Long> getLikedCommentIds(Long userId, List<Long> commentIds) {
        if (userId == null || commentIds.isEmpty()) return Set.of();
        return new HashSet<>(commentLikeRepository.findLikedCommentIdsByUserAndCommentIds(userId, commentIds));
    }

    @Override
    @Transactional
    public CommentLikeResult toggleLike(Long commentId, Long userId) {
        Comment comment = EntityLoader.getOrThrow(commentRepository::findById, commentId, "댓글");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");

        boolean liked = LikeToggler.toggle(
                () -> commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId),
                () -> commentRepository.decrementLikeCount(commentId),
                () -> {
                    commentLikeRepository.saveAndFlush(new CommentLike(comment, user));
                    commentRepository.incrementLikeCount(commentId);
                });
        // 원자적 UPDATE 이후 값을 다시 읽어야 정확하다 — 토글 직전 로드해둔 comment.getLikeCount()로
        // 계산하면 동시에 처리된 다른 사용자의 좋아요가 반영되지 않아 응답 값이 실제 DB 값과 어긋날 수 있다.
        // 엔티티 전체가 아니라 카운터만 스칼라로 다시 읽는다.
        Integer fresh = commentRepository.findLikeCountById(commentId);
        return new CommentLikeResult(liked, fresh != null ? fresh : 0);
    }

    @Override
    @Transactional
    public void removeLikesByUser(Long userId) {
        commentLikeRepository.decrementCommentLikeCountByUserId(userId);
        commentLikeRepository.deleteByUserId(userId);
    }
}
