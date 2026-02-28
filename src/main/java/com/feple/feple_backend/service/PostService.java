package com.feple.feple_backend.service;

import com.feple.feple_backend.domain.post.BoardType;
import com.feple.feple_backend.domain.post.Post;
import com.feple.feple_backend.domain.post.PostLike;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.dto.post.PostRequestDto;
import com.feple.feple_backend.dto.post.PostResponseDto;
import com.feple.feple_backend.repository.PostLikeRepository;
import com.feple.feple_backend.repository.PostRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createPost(PostRequestDto dto, User user){
        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .boardType(dto.getBoardType())
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .build();

        return postRepository.save(post).getId();
    }

    public List<PostResponseDto> getPostsByBoardType(BoardType boardType) {
        List<Post> posts = postRepository.findByBoardType(boardType);
        return posts.stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        Optional<PostLike> existing = postLikeRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.decrementLikeCount();
            return false; // 좋아요 취소
        } else {
            postLikeRepository.save(PostLike.builder().user(user).post(post).build());
            post.incrementLikeCount();
            return true; // 좋아요 추가
        }
    }

    @Transactional
    public void deletePost(Long postId) {
        postRepository.deleteById(postId);
    }
}
