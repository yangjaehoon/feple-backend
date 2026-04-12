package com.feple.feple_backend.comment.service;


import com.feple.feple_backend.comment.entity.Comment;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    public CommentResponseDto createComment(CreateCommentDto dto, Long userId) {
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("post not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Comment comment = new Comment(dto.getContent(), post, user);
        Comment saved = commentRepository.save(comment);
        return new CommentResponseDto(
                saved.getId(),
                post.getId(),
                user.getId(),
                user.getNickname(),
                saved.getContent(),
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId, PageRequest.of(0, 200))
                .stream()
                .map(c -> new CommentResponseDto(
                        c.getId(),
                        c.getPost().getId(),
                        c.getUser().getId(),
                        c.getUser().getNickname(),
                        c.getContent(),
                        c.getCreatedAt()
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
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getUser().getId().equals(requestUserId)) {
            throw new AccessDeniedException("본인의 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.deleteById(commentId);
    }
}
