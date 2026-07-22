package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.LikeToggler;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostLike;
import com.feple.feple_backend.post.event.PostLikedEvent;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public boolean isLikedByUser(Long postId, Long userId) {
        if (userId == null) return false;
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        Post post = EntityLoader.getOrThrow(postRepository::findById, postId, "게시글");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");

        return LikeToggler.toggle(
                () -> postLikeRepository.deleteByUserIdAndPostId(userId, postId),
                () -> postRepository.decrementLikeCount(postId),
                () -> {
                    postLikeRepository.saveAndFlush(new PostLike(user, post));
                    postRepository.incrementLikeCount(postId);
                    if (!post.getUserId().equals(userId)) {
                        eventPublisher.publishEvent(new PostLikedEvent(post.getUserId(), user.getNickname(), post.getTitle(), postId, userId));
                    }
                });
    }
}
