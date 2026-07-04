package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostActivityServiceImpl implements PostActivityService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    @Override
    public List<PostResponseDto> getMyPosts(Long userId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, PageSize.MY_ACTIVITIES))
                .stream().map(PostResponseDto::from).toList();
    }

    @Override
    public CursorPage<PostResponseDto> getMyPostsPaged(Long userId, Long cursor, int size) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        int page = CursorPage.toPage(cursor);
        Page<Post> result = postRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(page, size));
        List<PostResponseDto> content = result.getContent().stream().map(PostResponseDto::from).toList();
        return CursorPage.of(result, content, cursor);
    }

    @Override
    public CursorPage<PostResponseDto> getPublicPostsPaged(Long userId, Long cursor, int size) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        int page = CursorPage.toPage(cursor);
        Page<Post> result = postRepository.findPublicByUserOrderByCreatedAtDesc(user, PageRequest.of(page, size));
        List<PostResponseDto> content = result.getContent().stream().map(PostResponseDto::from).toList();
        return CursorPage.of(result, content, cursor);
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
