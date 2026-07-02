package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.photo.service.ArtistPhotoReportService;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.comment.service.CommentReportService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostReportService;
import com.feple.feple_backend.post.service.PostScrapService;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.user.dto.UserStatsDto;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final PostService postService;
    private final PostScrapService postScrapService;
    private final CommentService commentService;
    private final FestivalService festivalService;
    private final ArtistService artistService;
    private final PostReportService postReportService;
    private final CommentReportService commentReportService;
    private final ArtistPhotoReportService photoReportService;
    private final FestivalCertificationService certificationService;

    public List<PostResponseDto> getMyPosts(@NonNull Long userId) {
        return postService.getMyPosts(userId);
    }

    public CursorPage<PostResponseDto> getMyPostsPaged(@NonNull Long userId, Long cursor, int size) {
        return postService.getMyPostsPaged(userId, cursor, size);
    }

    public CursorPage<PostResponseDto> getPublicPostsPaged(@NonNull Long userId, Long cursor, int size) {
        return postService.getPublicPostsPaged(userId, cursor, size);
    }

    public List<PostResponseDto> getLikedPosts(@NonNull Long userId) {
        return postService.getLikedPosts(userId);
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

    public Map<Long, Long> getReportCounts(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<Long, Long> counts = new HashMap<>();
        postReportService.getAuthorReportCounts(userIds).forEach((id, cnt) -> counts.merge(id, cnt, Long::sum));
        commentReportService.getAuthorReportCounts(userIds).forEach((id, cnt) -> counts.merge(id, cnt, Long::sum));
        photoReportService.getAuthorReportCounts(userIds).forEach((id, cnt) -> counts.merge(id, cnt, Long::sum));
        return counts;
    }

    public UserStatsDto getUserStats(@NonNull Long userId) {
        long reportCount = postReportService.getReportCountForUser(userId)
                + commentReportService.getReportCountForUser(userId)
                + photoReportService.getReportCountForUser(userId);
        return new UserStatsDto(
                postService.countMyPosts(userId),
                commentService.countMyComments(userId),
                reportCount,
                postService.countLikedPosts(userId),
                postScrapService.countMyScraps(userId),
                certificationService.countApprovedByUser(userId));
    }
}
