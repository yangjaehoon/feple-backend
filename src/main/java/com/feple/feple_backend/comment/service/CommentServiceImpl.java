package com.feple.feple_backend.comment.service;


import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.notification.service.NotificationService;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.comment.dto.CommentResponseDto;
import com.feple.feple_backend.comment.dto.CreateCommentDto;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FestivalCertificationRepository certificationRepository;

    @Override
    public CommentResponseDto createComment(CreateCommentDto dto, Long userId) {
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        Comment comment = new Comment(dto.getContent(), post, user);
        Comment saved = commentRepository.save(comment);

        // 게시글 작성자 != 댓글 작성자일 때만 알림
        Long postAuthorId = post.getUser().getId();
        if (!postAuthorId.equals(userId)) {
            notificationService.notifyNewComment(
                    postAuthorId, user.getNickname(), post.getTitle(), post.getId());
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
                user.getRole()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다."));

        Set<Long> certifiedUserIds = new HashSet<>();
        if (post.getFestival() != null) {
            certifiedUserIds = certificationRepository.findApprovedUserIdsByFestivalId(post.getFestival().getId());
        }

        final Set<Long> finalCertifiedIds = certifiedUserIds;
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(0, 200))
                .stream()
                .map(c -> new CommentResponseDto(
                        c.getId(),
                        c.getPost().getId(),
                        c.getUser().getId(),
                        c.getUser().getNickname(),
                        c.getContent(),
                        c.getCreatedAt(),
                        finalCertifiedIds.contains(c.getUser().getId()),
                        c.getUser().getRole()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteComment(Long commentId){
        commentRepository.deleteById(commentId);
    }

    @Override
    public void deleteOwnComment(Long commentId, Long requestUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다."));
        if (!comment.getUser().getId().equals(requestUserId)) {
            throw new AccessDeniedException("본인의 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.deleteById(commentId);
    }
}
