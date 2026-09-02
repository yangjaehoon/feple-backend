package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.photo.service.ArtistPhotoReportService;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.comment.service.CommentReportService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostReportService;
import com.feple.feple_backend.post.service.PostScrapService;
import com.feple.feple_backend.post.service.UserPostHistoryService;
import com.feple.feple_backend.user.dto.UserStatsDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserPostHistoryService postActivityService;
    private final PostScrapService postScrapService;
    private final CommentService commentService;
    private final FestivalService festivalService;
    private final ArtistService artistService;
    private final PostReportService postReportService;
    private final CommentReportService commentReportService;
    private final ArtistPhotoReportService photoReportService;
    private final FestivalCertificationService certificationService;

    public List<PostResponseDto> getMyPosts(@NonNull Long userId) {
        return postActivityService.getMyPosts(userId);
    }

    public CursorPage<PostResponseDto> getMyPostsPaged(@NonNull Long userId, Long cursor, int size) {
        return postActivityService.getMyPostsPaged(userId, cursor, size);
    }

    public CursorPage<PostResponseDto> getPublicPostsPaged(@NonNull Long userId, Long cursor, int size) {
        return postActivityService.getPublicPostsPaged(userId, cursor, size);
    }

    public List<PostResponseDto> getLikedPosts(@NonNull Long userId) {
        return postActivityService.getLikedPosts(userId);
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

    // 신고 집계 소스(게시글/댓글/아티스트 사진) — getReportCounts/getUserStats가 함께 참조하는 단일 출처.
    // 새 신고 유형이 추가되면 여기 한 곳만 늘리면 된다.
    private List<Function<List<Long>, Map<Long, Long>>> reportCountSources() {
        return List.of(
                postReportService::getAuthorReportCounts,
                commentReportService::getAuthorReportCounts,
                photoReportService::getAuthorReportCounts
        );
    }

    public Map<Long, Long> getReportCounts(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<Long, Long> counts = new HashMap<>();
        reportCountSources().forEach(source ->
                source.apply(userIds).forEach((id, cnt) -> counts.merge(id, cnt, Long::sum)));
        return counts;
    }

    public UserStatsDto getUserStats(@NonNull Long userId) {
        return buildStats(userId, postActivityService.countPublicPosts(userId));
    }

    // 관리자 회원 상세용 — 같은 화면의 "최근 게시글" 목록이 익명 글도 보여주므로
    // 게시글 카운트도 익명 포함(countVisiblePosts)으로 맞춰 목록과 숫자가 어긋나지 않게 한다.
    // 공개 엔드포인트(GET /users/{id}/stats)는 익명 저작자 노출 방지를 위해 getUserStats를 그대로 쓴다.
    public UserStatsDto getUserStatsForAdmin(@NonNull Long userId) {
        return buildStats(userId, postActivityService.countVisiblePosts(userId));
    }

    private UserStatsDto buildStats(Long userId, long postCount) {
        long reportCount = reportCountSources().stream()
                .mapToLong(source -> source.apply(List.of(userId)).getOrDefault(userId, 0L))
                .sum();
        return new UserStatsDto(
                postCount,
                commentService.countMyComments(userId),
                reportCount,
                postActivityService.countLikedPosts(userId),
                postScrapService.countMyScraps(userId),
                certificationService.countApprovedByUser(userId));
    }
}
