package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.user.entity.User;

import java.util.List;

public interface PostService {
    Long createPost(PostRequestDto dto, Long userId);
    PostResponseDto getPost(Long postId);
    List<PostResponseDto> getHotPosts();
    List<PostResponseDto> getPostsByBoardType(BoardType boardType);
    List<PostResponseDto> getPostsByBoardTypePaged(BoardType boardType, int page, int size);
    void deleteOwnPost(Long postId, Long requestUserId);
    List<PostResponseDto> getPostsByArtistId(Long artistId);
    Long createArtistPost(Long artistId, PostRequestDto dto, Long userId);
    List<PostResponseDto> getPostsByFestivalId(Long festivalId);
    Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId);
    void deletePostsByFestival(Festival festival);
    void deletePostsByUser(User user);
    void deletePostsByArtist(Artist artist);
    List<PostResponseDto> searchPosts(String keyword);
    List<PostResponseDto> getMyPosts(Long userId);
    long countMyPosts(Long userId);
}
