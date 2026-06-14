package com.feple.feple_backend.comment.service;


import com.feple.feple_backend.badword.BadWordFilter;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
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
import com.feple.feple_backend.global.CountRowMapper;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.PermissionValidator;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
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
    private final FestivalCertificationRepository certificationRepository;
    private final BadWordFilter badWordFilter;

    @Override
    @Transactional
    public CommentResponseDto createComment(CreateCommentDto dto, Long userId) {
        badWordFilter.validateField("content", dto.getContent());
        Post post = EntityFinder.getOrThrow(postRepository::findById, dto.getPostId(), "게시글");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");

        Comment parent = null;
        if (dto.getParentId() != null) {
            parent = EntityFinder.getOrThrow(commentRepository::findById, dto.getParentId(), "부모 댓글");
        }
        Comment comment = new Comment(dto.getContent(), post, user, parent, dto.isAnonymous());
        Comment saved = commentRepository.save(comment);
        postRepository.incrementCommentCount(post.getId());

        Long postAuthorId = post.getUserId();
        if (!postAuthorId.equals(userId)) {
            eventPublisher.publishEvent(
                    new CommentCreatedEvent(postAuthorId, user.getNickname(), post.getTitle(), post.getId()));
        }

        boolean certified = post.getFestivalId() != null &&
                certificationRepository.existsApprovedCertification(post.getFestivalId(), userId);

        return CommentResponseDto.from(saved, certified, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByPost(Long postId, Long userId) {
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(0, PageSize.COMMENTS)).getContent();
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();

        Set<Long> certifiedUserIds = getCertifiedUserIds(post);
        Set<Long> likedCommentIds = getLikedCommentIds(userId, commentIds);

        return comments.stream()
                .map(c -> CommentResponseDto.from(
                        c,
                        certifiedUserIds.contains(c.getUserId()),
                        likedCommentIds.contains(c.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyCommentResponseDto> getMyComments(Long userId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
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
        Comment comment = EntityFinder.getOrThrow(commentRepository::findById, commentId, "댓글");
        // soft delete: 신고 기록(CommentReport) 보존, 행이 남아 FK 무결성 유지
        commentRepository.deleteById(commentId);
        postRepository.decrementCommentCount(comment.getPostId());
    }

    @Override
    @Transactional
    public void deleteOwnComment(Long commentId, Long requestUserId) {
        Comment comment = EntityFinder.getOrThrow(commentRepository::findById, commentId, "댓글");
        PermissionValidator.checkOwner(comment.getUserId(), requestUserId, "댓글");
        commentRepository.deleteById(commentId);
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
        return CountRowMapper.toLongMap(commentRepository.countGroupByUserId(userIds));
    }

    @Override
    @Transactional
    public void updateOwnComment(Long commentId, Long requestUserId, String content) {
        Comment comment = EntityFinder.getOrThrow(commentRepository::findById, commentId, "댓글");
        PermissionValidator.checkOwner(comment.getUserId(), requestUserId, "댓글");
        badWordFilter.validateField("content", content);
        comment.update(content);
    }

    private Set<Long> getCertifiedUserIds(Post post) {
        if (post.getFestivalId() == null) return Set.of();
        return certificationRepository.findApprovedUserIdsByFestivalId(post.getFestivalId());
    }

    @Override
    @Transactional
    public void deleteByPostIds(List<Long> postIds) {
        if (postIds.isEmpty()) return;
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
        Comment comment = EntityFinder.getOrThrow(commentRepository::findById, commentId, "댓글");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");

        boolean alreadyLiked = commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
        int currentCount = comment.getLikeCount();
        if (alreadyLiked) {
            commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
            commentRepository.decrementLikeCount(commentId);
            return new CommentLikeResult(false, currentCount - 1);
        } else {
            commentLikeRepository.save(new CommentLike(comment, user));
            commentRepository.incrementLikeCount(commentId);
            return new CommentLikeResult(true, currentCount + 1);
        }
    }
}
