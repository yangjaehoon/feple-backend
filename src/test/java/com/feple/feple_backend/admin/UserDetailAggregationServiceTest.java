package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.user.UserDetailAggregationService;
import com.feple.feple_backend.admin.user.UserDetailDto;
import com.feple.feple_backend.admin.user.UserListCountsDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.user.service.MyPageService;
import com.feple.feple_backend.user.service.UserAdminService;
import com.feple.feple_backend.userblock.service.UserBlockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserDetailAggregationServiceTest {

    @Mock UserAdminService userAdminService;
    @Mock MyPageService myPageService;
    @Mock CommentService commentService;
    @Mock PostAdminService postAdminService;
    @Mock UserBlockService userBlockService;
    @Mock FestivalCertificationAdminService certificationAdminService;

    @InjectMocks UserDetailAggregationService service;

    // ── getDetail ─────────────────────────────────────────────────────────────

    @Test
    void getDetail_모든_서비스_호출_후_모델_반환() {
        Long userId = 1L;
        UserResponseDto user = mock(UserResponseDto.class);
        UserStatsDto stats = mock(UserStatsDto.class);
        given(userAdminService.getAdminUser(userId)).willReturn(user);
        given(myPageService.getUserStats(userId)).willReturn(stats);
        given(postAdminService.getRecentPostsByUser(userId, 10)).willReturn(List.of());
        given(commentService.getRecentCommentsByUser(userId, 10)).willReturn(List.of());
        given(myPageService.getLikedFestivals(userId)).willReturn(List.of());
        given(myPageService.getFollowedArtists(userId)).willReturn(List.of());
        given(userBlockService.getBlockedUsers(userId)).willReturn(List.of());
        given(certificationAdminService.getByUserId(userId)).willReturn(List.of());

        UserDetailDto model = service.getDetail(userId);

        assertThat(model.user()).isSameAs(user);
        assertThat(model.stats()).isSameAs(stats);
    }

    @Test
    void getDetail_최근_게시글_댓글은_limit_10으로_조회() {
        Long userId = 2L;
        given(userAdminService.getAdminUser(userId)).willReturn(mock(UserResponseDto.class));
        given(myPageService.getUserStats(userId)).willReturn(mock(UserStatsDto.class));
        given(postAdminService.getRecentPostsByUser(userId, 10)).willReturn(List.of());
        given(commentService.getRecentCommentsByUser(userId, 10)).willReturn(List.of());
        given(myPageService.getLikedFestivals(userId)).willReturn(List.of());
        given(myPageService.getFollowedArtists(userId)).willReturn(List.of());
        given(userBlockService.getBlockedUsers(userId)).willReturn(List.of());
        given(certificationAdminService.getByUserId(userId)).willReturn(List.of());

        service.getDetail(userId);

        verify(postAdminService).getRecentPostsByUser(userId, 10);
        verify(commentService).getRecentCommentsByUser(userId, 10);
    }

    @Test
    void getDetail_서비스_결과가_모델_모든_필드에_반영됨() {
        Long userId = 3L;
        List<PostResponseDto> posts = List.of(mock(PostResponseDto.class));
        List<MyCommentResponseDto> comments = List.of(mock(MyCommentResponseDto.class));
        List<FestivalResponseDto> festivals = List.of(mock(FestivalResponseDto.class));
        List<ArtistResponseDto> artists = List.of(mock(ArtistResponseDto.class));
        given(userAdminService.getAdminUser(userId)).willReturn(mock(UserResponseDto.class));
        given(myPageService.getUserStats(userId)).willReturn(mock(UserStatsDto.class));
        given(postAdminService.getRecentPostsByUser(userId, 10)).willReturn(posts);
        given(commentService.getRecentCommentsByUser(userId, 10)).willReturn(comments);
        given(myPageService.getLikedFestivals(userId)).willReturn(festivals);
        given(myPageService.getFollowedArtists(userId)).willReturn(artists);
        given(userBlockService.getBlockedUsers(userId)).willReturn(List.of());
        given(certificationAdminService.getByUserId(userId)).willReturn(List.of());

        UserDetailDto model = service.getDetail(userId);

        assertThat(model.recentPosts()).isSameAs(posts);
        assertThat(model.recentComments()).isSameAs(comments);
        assertThat(model.likedFestivals()).isSameAs(festivals);
        assertThat(model.followedArtists()).isSameAs(artists);
    }

    // ── getListCounts ─────────────────────────────────────────────────────────

    @Test
    void getListCounts_세_카운트맵_모두_포함() {
        List<Long> userIds = List.of(1L, 2L, 3L);
        Map<Long, Long> reportCounts   = Map.of(1L, 2L, 2L, 0L);
        Map<Long, Long> postCounts     = Map.of(1L, 5L, 3L, 1L);
        Map<Long, Long> commentCounts  = Map.of(2L, 3L);
        given(myPageService.getReportCounts(userIds)).willReturn(reportCounts);
        given(postAdminService.getPostCountsByUserIds(userIds)).willReturn(postCounts);
        given(commentService.getCommentCountsByUserIds(userIds)).willReturn(commentCounts);

        UserListCountsDto model = service.getListCounts(userIds);

        assertThat(model.reportCounts()).isSameAs(reportCounts);
        assertThat(model.postCounts()).isSameAs(postCounts);
        assertThat(model.commentCounts()).isSameAs(commentCounts);
    }

    @Test
    void getListCounts_서비스에_userIds_그대로_전달() {
        List<Long> userIds = List.of(10L, 20L);
        given(myPageService.getReportCounts(userIds)).willReturn(Map.of());
        given(postAdminService.getPostCountsByUserIds(userIds)).willReturn(Map.of());
        given(commentService.getCommentCountsByUserIds(userIds)).willReturn(Map.of());

        service.getListCounts(userIds);

        verify(myPageService).getReportCounts(userIds);
        verify(postAdminService).getPostCountsByUserIds(userIds);
        verify(commentService).getCommentCountsByUserIds(userIds);
    }
}
