package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.user.dto.UserStatsDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final PostService postService;
    private final CommentService commentService;
    private final FestivalService festivalService;
    private final ArtistService artistService;

    public List<PostResponseDto> getMyPosts(@NonNull Long userId) {
        return postService.getMyPosts(userId);
    }

    public List<MyCommentResponseDto> getMyComments(@NonNull Long userId) {
        return commentService.getMyComments(userId);
    }

    public List<FestivalResponseDto> getLikedFestivals(@NonNull Long userId) {
        return festivalService.getLikedFestivals(userId);
    }

    public List<ArtistResponseDto> getFollowedArtists(@NonNull Long userId) {
        return artistService.getFollowedArtists(userId);
    }

    public UserStatsDto getUserStats(@NonNull Long userId) {
        return new UserStatsDto(
                postService.countMyPosts(userId),
                commentService.countMyComments(userId));
    }
}
