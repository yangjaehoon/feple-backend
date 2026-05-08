package com.feple.feple_backend.post.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PostService {
    Long createPost(PostRequestDto dto, Long userId);
    PostResponseDto getPost(Long postId);
    List<PostResponseDto> getHotPosts();
    List<PostResponseDto> getPostsByBoardType(BoardType boardType);
    Page<PostResponseDto> getPostsForAdmin(int page, int size, String filter, String keyword);
    long getTotalPostCount();
    long countRecentPosts(int days);
    List<PostResponseDto> getAdminHotPosts(int limit);
    void deleteOwnPost(Long postId, Long requestUserId);
    void deletePost(Long postId);
    void bulkDeletePosts(List<Long> ids);
    List<PostResponseDto> getPostsByArtistId(Long artistId);
    Long createArtistPost(Long artistId, PostRequestDto dto, Long userId);
    List<PostResponseDto> getPostsByFestivalId(Long festivalId);
    Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId);
    void deletePostsByFestival(Festival festival);
    List<PostResponseDto> searchPosts(String keyword);
}
