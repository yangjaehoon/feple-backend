package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;

import java.util.List;

public interface UserPostHistoryService {
    List<PostResponseDto> getMyPosts(Long userId);
    CursorPage<PostResponseDto> getMyPostsPaged(Long userId, Long cursor, int size);
    CursorPage<PostResponseDto> getPublicPostsPaged(Long userId, Long cursor, int size);
    long countPublicPosts(Long userId);
    List<PostResponseDto> getLikedPosts(Long userId);
    long countLikedPosts(Long userId);
}
