package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;

import java.util.List;

public interface PostService {
    Long createPost(PostRequestDto dto, Long userId);
    PostResponseDto getPost(Long postId);
    List<PostResponseDto> getHotPosts();
    List<PostResponseDto> getPostsByBoardType(BoardType boardType);
    List<PostResponseDto> getPostsByBoardTypePaged(BoardType boardType, int page, int size);
    List<PostResponseDto> getPostsByBoardTypePopular(BoardType boardType, int page, int size);
    void deleteOwnPost(Long postId, Long requestUserId);
    void updateOwnPost(Long postId, PostRequestDto dto, Long requestUserId);
    List<PostResponseDto> getPostsByArtistId(Long artistId);
    Long createArtistPost(Long artistId, PostRequestDto dto, Long userId);
    List<PostResponseDto> getPostsByFestivalId(Long festivalId);
    Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId);
    List<PostResponseDto> getPostsByFestivalIdAndBoardType(Long festivalId, BoardType boardType);
    Long createFestivalTypedPost(Long festivalId, PostRequestDto dto, Long userId, BoardType boardType);
    List<PostResponseDto> getPopularFestivalPosts(Long festivalId);
    List<PostResponseDto> searchPosts(String keyword, String boardType);
    List<PostResponseDto> getMyPosts(Long userId);
    long countMyPosts(Long userId);
    int incrementViewCount(Long postId);
    List<PostResponseDto> getLikedPosts(Long userId);
    long countLikedPosts(Long userId);
}
