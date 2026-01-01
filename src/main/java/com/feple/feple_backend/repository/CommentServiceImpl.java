package com.feple.feple_backend.repository;


import com.feple.feple_backend.domain.comment.Comment;
import com.feple.feple_backend.domain.post.Post;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.dto.comment.CommentResponseDto;
import com.feple.feple_backend.dto.comment.CreateCommentDto;
import com.feple.feple_backend.service.CommentService;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
    public CommentResponseDto createComment(CreateCommentDto dto) {
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("post not found"));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Comment comment = new Comment(dto.getContent(), post, user);
        Comment saved = commentRepository.save(comment);
        return new CommentResponseDto(
                saved.getId(),
                post.getId(),
                user.getId(),
                saved.getContent(),
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByPost(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(c-> new CommentResponseDto(
                        c.getId(),
                        c.getPost().getId(),
                        c.getUser().getId(),
                        c.getContent(),
                        c.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteComment(Long commentId){
        commentRepository.deleteById(commentId);
    }
}
