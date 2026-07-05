package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.service.AdminDashboardMetrics;
import com.feple.feple_backend.admin.service.AdminPendingItemsService;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.post.service.PostAdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminDashboardAssemblerTest {

    @Mock FestivalAdminService festivalService;
    @Mock ArtistAdminService artistService;
    @Mock PostAdminService postAdminService;
    @Mock AdminDashboardMetrics adminMetricsService;
    @Mock AdminPendingItemsService adminPendingItemsService;
    @Mock ArtistSuggestionAdminService artistSuggestionAdminService;

    @InjectMocks AdminDashboardAssembler assembler;

    @Test
    void assemble_모든_서비스에서_데이터를_수집해_DTO로_반환() {
        given(festivalService.getTotalCount()).willReturn(10L);
        given(artistService.getTotalCount()).willReturn(20L);
        given(postAdminService.getTotalPostCount()).willReturn(30L);
        given(adminMetricsService.getTotalUserCount()).willReturn(40L);
        given(postAdminService.countRecentPosts(AdminConstants.STATS_RECENT_DAYS)).willReturn(5L);

        given(adminPendingItemsService.getPendingCerts(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(adminPendingItemsService.getPendingCertCount()).willReturn(2L);
        given(adminPendingItemsService.getPendingReports(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(adminPendingItemsService.getPendingReportCount()).willReturn(3L);
        given(adminPendingItemsService.getPendingSongRequests(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(adminPendingItemsService.getPendingSongRequestCount()).willReturn(1L);
        given(artistSuggestionAdminService.getPendingSuggestionsPreview(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(artistSuggestionAdminService.countPending()).willReturn(4L);

        given(postAdminService.getAdminHotPosts(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(artistService.getTopArtists(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(adminMetricsService.getRecentUsers()).willReturn(List.of());
        given(adminMetricsService.getDailyStats()).willReturn(List.of());

        AdminDashboardDto dto = assembler.assemble();

        assertThat(dto).isNotNull();
        assertThat(dto.stats().totalFestivals()).isEqualTo(10L);
        assertThat(dto.stats().totalArtists()).isEqualTo(20L);
        assertThat(dto.stats().totalPosts()).isEqualTo(30L);
        assertThat(dto.stats().totalUsers()).isEqualTo(40L);
        assertThat(dto.stats().recentPostCount()).isEqualTo(5L);
        assertThat(dto.pending().certCount()).isEqualTo(2L);
        assertThat(dto.pending().reportCount()).isEqualTo(3L);
        assertThat(dto.pending().songRequestCount()).isEqualTo(1L);
        assertThat(dto.pending().artistSuggestionCount()).isEqualTo(4L);
    }

    @Test
    void assemble_DASHBOARD_PREVIEW_SIZE_상수로_서비스_호출() {
        given(festivalService.getTotalCount()).willReturn(0L);
        given(artistService.getTotalCount()).willReturn(0L);
        given(postAdminService.getTotalPostCount()).willReturn(0L);
        given(adminMetricsService.getTotalUserCount()).willReturn(0L);
        given(postAdminService.countRecentPosts(AdminConstants.STATS_RECENT_DAYS)).willReturn(0L);
        given(adminPendingItemsService.getPendingCerts(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(adminPendingItemsService.getPendingCertCount()).willReturn(0L);
        given(adminPendingItemsService.getPendingReports(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(adminPendingItemsService.getPendingReportCount()).willReturn(0L);
        given(adminPendingItemsService.getPendingSongRequests(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(adminPendingItemsService.getPendingSongRequestCount()).willReturn(0L);
        given(artistSuggestionAdminService.getPendingSuggestionsPreview(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(artistSuggestionAdminService.countPending()).willReturn(0L);
        given(postAdminService.getAdminHotPosts(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(artistService.getTopArtists(AdminConstants.DASHBOARD_PREVIEW_SIZE)).willReturn(List.of());
        given(adminMetricsService.getRecentUsers()).willReturn(List.of());
        given(adminMetricsService.getDailyStats()).willReturn(List.of());

        assembler.assemble();

        then(adminPendingItemsService).should().getPendingCerts(AdminConstants.DASHBOARD_PREVIEW_SIZE);
        then(adminPendingItemsService).should().getPendingReports(AdminConstants.DASHBOARD_PREVIEW_SIZE);
        then(adminPendingItemsService).should().getPendingSongRequests(AdminConstants.DASHBOARD_PREVIEW_SIZE);
        then(artistSuggestionAdminService).should().getPendingSuggestionsPreview(AdminConstants.DASHBOARD_PREVIEW_SIZE);
        then(postAdminService).should().countRecentPosts(AdminConstants.STATS_RECENT_DAYS);
    }
}
