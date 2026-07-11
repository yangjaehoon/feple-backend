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
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.EntityRequirer;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.OwnershipValidator;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import com.feple.feple_backend.userblock.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentReportRepository commentReportRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FestivalCertificationService certificationService;
    private final BadWordValidator badWordFilter;
    private final UserBlockService userBlockService;
    private final BlockedContentFilter blockedContentFilter;

    @Override
    @Transactional
    public CommentResponseDto createComment(CreateCommentDto dto, Long userId) {
        badWordFilter.validateField("content", dto.getContent());
        Post post = EntityRequirer.getOrThrow(postRepository::findById, dto.getPostId(), "게시글");

        if (userBlockService.isBlocked(post.getUserId(), userId)) {
            throw new AccessDeniedException("차단된 사용자의 게시글에는 댓글을 작성할 수 없습니다.");
        }

        User user = EntityRequirer.getOrThrow(userRepository::findById, userId, "사용자");

        Comment parent = null;
        if (dto.getParentId() != null) {
            parent = EntityRequirer.getOrThrow(commentRepository::findById, dto.getParentId(), "부모 댓글");
        }
        Comment comment = new Comment(dto.getContent(), post, user, parent, dto.isAnonymous());
        Comment saved = commentRepository.save(comment);
        postRepository.incrementCommentCount(post.getId());

        Long postAuthorId = post.getUserId();
        String commenterName = dto.isAnonymous() ? "익명" : user.getNickname();

        Long parentCommentAuthorId = resolveParentCommentAuthorId(parent, userId, postAuthorId);

        if (!postAuthorId.equals(userId)) {
            eventPublisher.publishEvent(
                    new CommentCreatedEvent(postAuthorId, commenterName, post.getTitle(), post.getId(), parentCommentAuthorId, userId));
        } else if (parentCommentAuthorId != null) {
            // 게시글 작성자가 자기 게시글에 댓글을 달아도 원댓글 작성자에게는 알림 필요
            eventPublisher.publishEvent(
                    new CommentCreatedEvent(null, commenterName, post.getTitle(), post.getId(), parentCommentAuthorId, userId));
        } else {
            // 게시글 작성자가 자기 게시글에 최상위 댓글 — 알림 없음, 포인트만 지급
            eventPublisher.publishEvent(
                    new CommentCreatedEvent(null, commenterName, post.getTitle(), post.getId(), null, userId));
        }

        boolean certified = post.getFestivalId() != null &&
                certificationService.existsApprovedCertification(post.getFestivalId(), userId);

        return CommentResponseDto.from(saved, certified, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByPost(Long postId, Long userId) {
        Post post = EntityRequirer.getOrThrow(postRepository::findById, postId, "게시글");
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(0, PageSize.COMMENTS)).getContent();
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();

        Set<Long> certifiedUserIds = getCertifiedUserIds(post);
        Set<Long> likedCommentIds = getLikedCommentIds(userId, commentIds);

        List<CommentResponseDto> result = comments.stream()
                .map(c -> CommentResponseDto.from(
                        c,
                        certifiedUserIds.contains(c.getUserId()),
                        likedCommentIds.contains(c.getId())))
                .toList();
        return blockedContentFilter.excludeBlocked(result, userId, CommentResponseDto::getUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getAdminCommentsByPost(Long postId, int limit) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(0, limit))
                .getContent().stream()
                .map(c -> CommentResponseDto.from(c, false, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyCommentResponseDto> getMyComments(Long userId) {
        User user = EntityRequirer.getOrThrow(userRepository::findById, userId, "사용자");
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
    public void deleteComment(Long commentId){
        // soft delete: 신고 기록(CommentReport) 보존, 행이 남아 FK 무결성 유지
        deleteAndDecrement(EntityRequirer.getOrThrow(commentRepository::findById, commentId, "댓글"));
    }

    @Override
    @Transactional
    public void deleteOwnComment(Long commentId, Long requestUserId) {
        Comment comment = EntityRequirer.getOrThrow(commentRepository::findById, commentId, "댓글");
        OwnershipValidator.checkOwner(comment.getUserId(), requestUserId, "댓글");
        deleteAndDecrement(comment);
    }

    private Long resolveParentCommentAuthorId(Comment parent, Long userId, Long postAuthorId) {
        if (parent == null) return null;
        Long parentAuthorId = parent.getUserId();
        if (parentAuthorId.equals(userId) || parentAuthorId.equals(postAuthorId)) return null;
        return parentAuthorId;
    }

    private void deleteAndDecrement(Comment comment) {
        commentRepository.deleteById(comment.getId());
        postRepository.decrementCommentCount(comment.getPostId());
    }

    @Override
    @Transactional(readOnly = true)
    public long countCommentsContaining(String word) {
        return commentRepository.countByContentContaining(word.toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, Long> getCommentCountsByUserIds(java.util.List<Long> userIds) {
        if (userIds.isEmpty()) return java.util.Map.of();
        return QueryResultMapper.toLongMap(commentRepository.countGroupByUserId(userIds));
    }

    @Override
    @Transactional
    public void updateOwnComment(Long commentId, Long requestUserId, String content) {
        Comment comment = EntityRequirer.getOrThrow(commentRepository::findById, commentId, "댓글");
        OwnershipValidator.checkOwner(comment.getUserId(), requestUserId, "댓글", "수정");
        badWordFilter.validateField("content", content);
        comment.update(content);
    }

    private Set<Long> getCertifiedUserIds(Post post) {
        if (post.getFestivalId() == null) return Set.of();
        return certificationService.findApprovedUserIdsByFestivalId(post.getFestivalId());
    }

    @Override
    @Transactional
    public void deleteByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) return;
        // FK 순서: CommentLike → CommentReport → Comment
        commentLikeRepository.deleteByPostIds(postIds);
        commentReportRepository.deleteByPostIds(postIds);
        commentRepository.deleteByPostIds(postIds);
    }

    private Set<Long> getLikedCommentIds(Long userId, List<Long> commentIds) {
        if (userId == null || commentIds.isEmpty()) return Set.of();
        return new HashSet<>(commentLikeRepository.findLikedCommentIdsByUserAndCommentIds(userId, commentIds));
    }

    @Override
    @Transactional
    public CommentLikeResult toggleLike(Long commentId, Long userId) {
        Comment comment = EntityRequirer.getOrThrow(commentRepository::findById, commentId, "댓글");
        User user = EntityRequirer.getOrThrow(userRepository::findById, userId, "사용자");

        int deleted = commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
        if (deleted > 0) {
            commentRepository.decrementLikeCount(commentId);
            return new CommentLikeResult(false, Math.max(0, comment.getLikeCount() - 1));
        }
        commentLikeRepository.save(new CommentLike(comment, user));
        commentRepository.incrementLikeCount(commentId);
        return new CommentLikeResult(true, comment.getLikeCount() + 1);
    }

    @Override
    @Transactional
    public void removeLikesByUser(Long userId) {
        commentLikeRepository.decrementCommentLikeCountByUserId(userId);
        commentLikeRepository.deleteByUserId(userId);
    }
}
