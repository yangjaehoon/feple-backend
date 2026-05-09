package com.feple.feple_backend.comment.service;


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
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FestivalCertificationRepository certificationRepository;

    @Override
    public CommentResponseDto createComment(CreateCommentDto dto, Long userId) {
        Post post = EntityFinder.getOrThrow(postRepository::findById, dto.getPostId(), "게시글");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");

        Comment comment = new Comment(dto.getContent(), post, user, dto.getParentId());
        Comment saved = commentRepository.save(comment);

        Long postAuthorId = post.getUserId();
        if (!postAuthorId.equals(userId)) {
            eventPublisher.publishEvent(
                    new CommentCreatedEvent(postAuthorId, user.getNickname(), post.getTitle(), post.getId()));
        }

        boolean certified = false;
        if (post.getFestival() != null) {
            certified = certificationRepository
                    .findApprovedUserIdsByFestivalId(post.getFestival().getId())
                    .contains(userId);
        }

        return new CommentResponseDto(
                saved.getId(),
                post.getId(),
                user.getId(),
                user.getNickname(),
                saved.getContent(),
                saved.getCreatedAt(),
                certified,
                user.getRole(),
                saved.getParentId(),
                0,
                false
        );
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
                .map(c -> new CommentResponseDto(
                        c.getId(),
                        c.getPostId(),
                        c.getUserId(),
                        c.getUserNickname(),
                        c.getContent(),
                        c.getCreatedAt(),
                        certifiedUserIds.contains(c.getUserId()),
                        c.getUserRole(),
                        c.getParentId(),
                        c.getLikeCount(),
                        likedCommentIds.contains(c.getId())
                ))
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
    public long countMyComments(Long userId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return commentRepository.countByUser(user);
    }

    @Override
    public void deleteComment(Long commentId){
        commentLikeRepository.deleteByCommentId(commentId);
        commentReportRepository.deleteByCommentId(commentId);
        commentRepository.deleteById(commentId);
    }

    @Override
    public void deleteOwnComment(Long commentId, Long requestUserId) {
        Comment comment = EntityFinder.getOrThrow(commentRepository::findById, commentId, "댓글");
        PermissionValidator.checkOwner(comment.getUserId(), requestUserId, "댓글");
        commentLikeRepository.deleteByCommentId(commentId);
        commentReportRepository.deleteByCommentId(commentId);
        commentRepository.deleteById(commentId);
    }

    private Set<Long> getCertifiedUserIds(Post post) {
        if (post.getFestival() == null) return Set.of();
        return certificationRepository.findApprovedUserIdsByFestivalId(post.getFestival().getId());
    }

    private Set<Long> getLikedCommentIds(Long userId, List<Long> commentIds) {
        if (userId == null || commentIds.isEmpty()) return Set.of();
        return new HashSet<>(commentLikeRepository.findLikedCommentIdsByUserAndCommentIds(userId, commentIds));
    }

    @Override
    public CommentLikeResult toggleLike(Long commentId, Long userId) {
        Comment comment = EntityFinder.getOrThrow(commentRepository::findById, commentId, "댓글");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");

        boolean alreadyLiked = commentLikeRepository.existsByUserIdAndCommentId(userId, commentId);
        if (alreadyLiked) {
            commentLikeRepository.deleteByUserIdAndCommentId(userId, commentId);
            comment.decrementLikeCount();
            return new CommentLikeResult(false, comment.getLikeCount());
        } else {
            commentLikeRepository.save(CommentLike.builder().comment(comment).user(user).build());
            comment.incrementLikeCount();
            return new CommentLikeResult(true, comment.getLikeCount());
        }
    }
}
