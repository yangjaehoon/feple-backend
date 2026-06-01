package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostAdminFilter;
import com.feple.feple_backend.post.dto.PostResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PostAdminService {
    Page<PostResponseDto> getPostsForAdmin(PostAdminFilter params);
    long getTotalPostCount();
    long countRecentPosts(int days);
    List<PostResponseDto> getAdminHotPosts(int limit);
    void deletePost(Long postId);
    void bulkDeletePosts(List<Long> ids);
    long countPostsContaining(String word);
    java.util.Map<Long, Long> getPostCountsByUserIds(java.util.List<Long> userIds);
    List<PostResponseDto> getDeletedPosts(int limit);
    void restorePost(Long postId);
}
