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
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostReportService;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.post.service.PostScrapService;
import com.feple.feple_backend.user.dto.UserStatsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock PostService postService;
    @Mock CommentService commentService;
    @Mock FestivalService festivalService;
    @Mock ArtistService artistService;
    @Mock PostReportService postReportService;
    @Mock CommentReportService commentReportService;
    @Mock ArtistPhotoReportService photoReportService;
    @Mock PostScrapService postScrapService;
    @Mock FestivalCertificationService certificationService;

    @InjectMocks MyPageService myPageService;

    // ── getMyPosts ────────────────────────────────────────────────────

    @Test
    void 내_게시글_목록_조회시_postService에_위임() {
        List<PostResponseDto> posts = List.of(mock(PostResponseDto.class));
        given(postService.getMyPosts(1L)).willReturn(posts);

        List<PostResponseDto> result = myPageService.getMyPosts(1L);

        assertThat(result).isEqualTo(posts);
        verify(postService).getMyPosts(1L);
    }

    @Test
    void userId_null이면_NullPointerException_발생() {
        assertThatThrownBy(() -> myPageService.getMyPosts(null))
                .isInstanceOf(NullPointerException.class);
    }

    // ── getMyComments ─────────────────────────────────────────────────

    @Test
    void 내_댓글_목록_조회시_commentService에_위임() {
        List<MyCommentResponseDto> comments = List.of(mock(MyCommentResponseDto.class));
        given(commentService.getMyComments(2L)).willReturn(comments);

        List<MyCommentResponseDto> result = myPageService.getMyComments(2L);

        assertThat(result).isEqualTo(comments);
        verify(commentService).getMyComments(2L);
    }

    // ── getLikedFestivals ─────────────────────────────────────────────

    @Test
    void 좋아요한_페스티벌_목록_조회시_festivalService에_위임() {
        List<FestivalResponseDto> festivals = List.of(mock(FestivalResponseDto.class));
        given(festivalService.getLikedFestivals(3L)).willReturn(festivals);

        List<FestivalResponseDto> result = myPageService.getLikedFestivals(3L);

        assertThat(result).isEqualTo(festivals);
        verify(festivalService).getLikedFestivals(3L);
    }

    // ── getFollowedArtists ────────────────────────────────────────────

    @Test
    void 팔로우한_아티스트_목록_조회시_artistService에_위임() {
        List<ArtistResponseDto> artists = List.of(mock(ArtistResponseDto.class));
        given(artistService.getFollowedArtists(4L)).willReturn(artists);

        List<ArtistResponseDto> result = myPageService.getFollowedArtists(4L);

        assertThat(result).isEqualTo(artists);
        verify(artistService).getFollowedArtists(4L);
    }

    // ── getUserStats ──────────────────────────────────────────────────

    @Test
    void 사용자_통계_게시글수와_댓글수_합산_반환() {
        given(postService.countMyPosts(1L)).willReturn(5L);
        given(commentService.countMyComments(1L)).willReturn(12L);
        given(postService.countLikedPosts(1L)).willReturn(0L);
        given(postScrapService.countMyScraps(1L)).willReturn(0L);
        given(certificationService.countApprovedByUser(1L)).willReturn(0L);
        given(postReportService.getReportCountForUser(1L)).willReturn(3L);
        given(commentReportService.getReportCountForUser(1L)).willReturn(2L);
        given(photoReportService.getReportCountForUser(1L)).willReturn(1L);

        UserStatsDto stats = myPageService.getUserStats(1L);

        assertThat(stats.getPostCount()).isEqualTo(5L);
        assertThat(stats.getCommentCount()).isEqualTo(12L);
        assertThat(stats.getReportCount()).isEqualTo(6L);
    }

    @Test
    void 게시글과_댓글이_없으면_통계가_0() {
        given(postService.countMyPosts(1L)).willReturn(0L);
        given(commentService.countMyComments(1L)).willReturn(0L);
        given(postService.countLikedPosts(1L)).willReturn(0L);
        given(postScrapService.countMyScraps(1L)).willReturn(0L);
        given(certificationService.countApprovedByUser(1L)).willReturn(0L);
        given(postReportService.getReportCountForUser(1L)).willReturn(0L);
        given(commentReportService.getReportCountForUser(1L)).willReturn(0L);
        given(photoReportService.getReportCountForUser(1L)).willReturn(0L);

        UserStatsDto stats = myPageService.getUserStats(1L);

        assertThat(stats.getPostCount()).isZero();
        assertThat(stats.getCommentCount()).isZero();
        assertThat(stats.getReportCount()).isZero();
    }
}
