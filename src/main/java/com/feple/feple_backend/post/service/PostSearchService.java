package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostResponseDto;

import java.util.List;

public interface PostSearchService {
    List<PostResponseDto> searchPosts(String keyword, String boardType, Long viewerId);
}
