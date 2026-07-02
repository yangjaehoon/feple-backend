package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;

import java.util.List;

public interface PostService {
    Long createPost(PostRequestDto dto, Long userId);
    PostResponseDto getPost(Long postId);
    List<PostResponseDto> getHotPosts();
    List<PostResponseDto> getPostsByBoardType(BoardType boardType);
    CursorPage<PostResponseDto> getPostsByBoardTypePaged(BoardType boardType, Long cursor, int size);
    CursorPage<PostResponseDto> getPostsByBoardTypePopular(BoardType boardType, Long cursor, int size);
    void deleteOwnPost(Long postId, Long requestUserId);
    void updateOwnPost(Long postId, PostRequestDto dto, Long requestUserId);
    List<PostResponseDto> getPostsByArtistId(Long artistId);
    CursorPage<PostResponseDto> getPostsByArtistIdPaged(Long artistId, Long cursor, int size);
    Long createArtistPost(Long artistId, PostRequestDto dto, Long userId);
    List<PostResponseDto> getPostsByFestivalId(Long festivalId);
    CursorPage<PostResponseDto> getPostsByFestivalIdPaged(Long festivalId, Long cursor, int size);
    Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId);
    List<PostResponseDto> getPostsByFestivalIdAndBoardType(Long festivalId, BoardType boardType);
    CursorPage<PostResponseDto> getPostsByFestivalIdAndBoardTypePaged(Long festivalId, BoardType boardType, Long cursor, int size);
    Long createFestivalTypedPost(Long festivalId, PostRequestDto dto, Long userId, BoardType boardType);
    List<PostResponseDto> getPopularFestivalPosts(Long festivalId);
    List<PostResponseDto> searchPosts(String keyword, String boardType);
    List<PostResponseDto> getMyPosts(Long userId);
    CursorPage<PostResponseDto> getMyPostsPaged(Long userId, Long cursor, int size);
    CursorPage<PostResponseDto> getPublicPostsPaged(Long userId, Long cursor, int size);
    long countMyPosts(Long userId);
    void incrementViewCount(Long postId);
    List<PostResponseDto> getLikedPosts(Long userId);
    long countLikedPosts(Long userId);
}
