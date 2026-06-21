package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostLike;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");

        int deleted = postLikeRepository.deleteByUserIdAndPostId(userId, postId);
        if (deleted > 0) {
            postRepository.decrementLikeCount(postId);
            return false;
        }
        postLikeRepository.save(new PostLike(user, post));
        postRepository.incrementLikeCount(postId);
        return true;
    }
}
