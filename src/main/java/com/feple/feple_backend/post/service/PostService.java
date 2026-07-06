package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;

import java.util.List;

public interface PostService {
    Long createPost(PostRequestDto dto, Long userId);
    PostResponseDto getPost(Long postId);
    List<PostResponseDto> getHotPosts(Long viewerId);
    CursorPage<PostResponseDto> getPostsByBoardTypePaged(BoardType boardType, Long cursor, int size, Long viewerId);
    CursorPage<PostResponseDto> getPostsByBoardTypePopular(BoardType boardType, Long cursor, int size, Long viewerId);
    void deleteOwnPost(Long postId, Long requestUserId);
    void updateOwnPost(Long postId, PostRequestDto dto, Long requestUserId);
    CursorPage<PostResponseDto> getPostsByArtistIdPaged(Long artistId, Long cursor, int size, Long viewerId);
    Long createArtistPost(Long artistId, PostRequestDto dto, Long userId);
    CursorPage<PostResponseDto> getPostsByFestivalIdPaged(Long festivalId, Long cursor, int size, Long viewerId);
    Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId);
    CursorPage<PostResponseDto> getPostsByFestivalIdAndBoardTypePaged(Long festivalId, BoardType boardType, Long cursor, int size, Long viewerId);
    Long createFestivalTypedPost(Long festivalId, PostRequestDto dto, Long userId, BoardType boardType);
    List<PostResponseDto> getPopularFestivalPosts(Long festivalId, Long viewerId);
    void incrementViewCount(Long postId);
}
