package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.CursorPageRequest;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;

import java.util.List;

public interface PostService {
    Long createPost(PostRequestDto dto, Long userId);
    PostResponseDto getPost(Long postId);
    List<PostResponseDto> getPopularPosts(Long viewerId);
    CursorPage<PostResponseDto> getPostsByBoardTypeLatest(BoardType boardType, CursorPageRequest pageRequest);
    CursorPage<PostResponseDto> getPostsByBoardTypePopular(BoardType boardType, CursorPageRequest pageRequest);
    void deleteOwnPost(Long postId, Long requestUserId);
    void updateOwnPost(Long postId, PostRequestDto dto, Long requestUserId);
    CursorPage<PostResponseDto> getPostsByArtistIdPaged(Long artistId, CursorPageRequest pageRequest);
    Long createArtistPost(Long artistId, PostRequestDto dto, Long userId);
    CursorPage<PostResponseDto> getPostsByFestivalIdPaged(Long festivalId, CursorPageRequest pageRequest);
    Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId);
    CursorPage<PostResponseDto> getPostsByFestivalIdAndBoardTypePaged(Long festivalId, BoardType boardType, CursorPageRequest pageRequest);
    Long createFestivalTypedPost(Long festivalId, PostRequestDto dto, Long userId, BoardType boardType);
    List<PostResponseDto> getPopularFestivalPosts(Long festivalId, Long viewerId);
    void incrementViewCount(Long postId);
}
