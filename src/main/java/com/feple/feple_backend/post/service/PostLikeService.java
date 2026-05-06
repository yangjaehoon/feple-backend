package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostLike;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public boolean isLikedByUser(Long postId, Long userId) {
        if (userId == null) return false;
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        Optional<PostLike> existing = postLikeRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            postRepository.decrementLikeCount(postId);
            return false;
        } else {
            postLikeRepository.save(PostLike.builder().user(user).post(post).build());
            postRepository.incrementLikeCount(postId);
            return true;
        }
    }
}
