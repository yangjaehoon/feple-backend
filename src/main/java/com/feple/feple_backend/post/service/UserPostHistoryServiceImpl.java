package com.feple.feple_backend.post.service;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPostHistoryServiceImpl implements UserPostHistoryService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    public List<PostResponseDto> getMyPosts(Long userId) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, PageSize.MY_ACTIVITIES))
                .stream().map(post -> PostResponseDto.from(post, fileStorageService)).toList();
    }

    @Override
    public CursorPage<PostResponseDto> getMyPostsPaged(Long userId, Long cursor, int size) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        return CursorPageAssembler.assemble(cursor, size,
                limit -> postRepository.findByUserOrderByIdDesc(user, limit),
                limit -> postRepository.findByUserAndIdLessThanOrderByIdDesc(user, cursor, limit),
                pageItems -> pageItems.stream().map(post -> PostResponseDto.from(post, fileStorageService)).toList(),
                Post::getId);
    }

    @Override
    public CursorPage<PostResponseDto> getPublicPostsPaged(Long userId, Long cursor, int size) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        return CursorPageAssembler.assemble(cursor, size,
                limit -> postRepository.findPublicByUserOrderByIdDesc(user, limit),
                limit -> postRepository.findPublicByUserAndIdLessThanOrderByIdDesc(user, cursor, limit),
                pageItems -> pageItems.stream().map(post -> PostResponseDto.from(post, fileStorageService)).toList(),
                Post::getId);
    }

    @Override
    public long countPublicPosts(Long userId) {
        return postRepository.countPublicByUserId(userId);
    }

    @Override
    public long countVisiblePosts(Long userId) {
        return postRepository.countByUserId(userId);
    }

    @Override
    public List<PostResponseDto> getLikedPosts(Long userId) {
        return postLikeRepository.findPostsByUserId(userId, PageRequest.of(0, PageSize.MY_ACTIVITIES))
                .stream()
                .map(post -> PostResponseDto.from(post, fileStorageService))
                .toList();
    }

    @Override
    public long countLikedPosts(Long userId) {
        return postLikeRepository.countByUserId(userId);
    }
}
