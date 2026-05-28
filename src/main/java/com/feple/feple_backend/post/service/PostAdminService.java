package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PostAdminService {
    Page<PostResponseDto> getPostsForAdmin(int page, int size, String filter, String keyword, Long artistId, Long festivalId);
    long getTotalPostCount();
    long countRecentPosts(int days);
    List<PostResponseDto> getAdminHotPosts(int limit);
    void deletePost(Long postId);
    void bulkDeletePosts(List<Long> ids);
    long countPostsContaining(String word);
}
