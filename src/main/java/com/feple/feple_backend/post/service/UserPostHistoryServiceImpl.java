package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPostHistoryServiceImpl implements UserPostHistoryService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    @Override
    public List<PostResponseDto> getMyPosts(Long userId) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, PageSize.MY_ACTIVITIES))
                .stream().map(PostResponseDto::from).toList();
    }

    @Override
    public CursorPage<PostResponseDto> getMyPostsPaged(Long userId, Long cursor, int size) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        int fetchSize = size + 1;
        PageRequest limit = PageRequest.of(0, fetchSize);
        List<Post> posts = (cursor == null)
                ? postRepository.findByUserOrderByIdDesc(user, limit)
                : postRepository.findByUserAndIdLessThanOrderByIdDesc(user, cursor, limit);
        boolean hasNext = posts.size() == fetchSize;
        List<PostResponseDto> content = posts.stream().limit(size).map(PostResponseDto::from).toList();
        Long nextCursor = hasNext && !content.isEmpty() ? content.get(content.size() - 1).getId() : null;
        return new CursorPage<>(content, nextCursor, hasNext);
    }

    @Override
    public CursorPage<PostResponseDto> getPublicPostsPaged(Long userId, Long cursor, int size) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        int fetchSize = size + 1;
        PageRequest limit = PageRequest.of(0, fetchSize);
        List<Post> posts = (cursor == null)
                ? postRepository.findPublicByUserOrderByIdDesc(user, limit)
                : postRepository.findPublicByUserAndIdLessThanOrderByIdDesc(user, cursor, limit);
        boolean hasNext = posts.size() == fetchSize;
        List<PostResponseDto> content = posts.stream().limit(size).map(PostResponseDto::from).toList();
        Long nextCursor = hasNext && !content.isEmpty() ? content.get(content.size() - 1).getId() : null;
        return new CursorPage<>(content, nextCursor, hasNext);
    }

    @Override
    public long countPublicPosts(Long userId) {
        return postRepository.countPublicByUserId(userId);
    }

    @Override
    public List<PostResponseDto> getLikedPosts(Long userId) {
        return postLikeRepository.findPostsByUserId(userId, PageRequest.of(0, PageSize.MY_ACTIVITIES))
                .stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Override
    public long countLikedPosts(Long userId) {
        return postLikeRepository.countByUserId(userId);
    }
}
