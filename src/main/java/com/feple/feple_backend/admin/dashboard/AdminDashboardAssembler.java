package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.service.AdminDashboardMetrics;
import com.feple.feple_backend.admin.service.AdminPendingItemsService;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.post.service.PostAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardAssembler {

    private final FestivalAdminService festivalService;
    private final ArtistAdminService artistService;
    private final PostAdminService postAdminService;
    private final AdminDashboardMetrics adminMetricsService;
    private final AdminPendingItemsService adminPendingItemsService;
    private final ArtistSuggestionAdminService artistSuggestionAdminService;

    public AdminDashboardDto assemble() {
        return new AdminDashboardDto(buildStats(), buildPending(), buildContent());
    }

    // 통계·처리대기·콘텐츠를 섹션별로 격리해 하나가 실패해도 나머지 섹션은 렌더링된다.

    private AdminStatsSummary buildStats() {
        try {
            return new AdminStatsSummary(
                    festivalService.getTotalCount(),
                    artistService.getTotalCount(),
                    postAdminService.getTotalPostCount(),
                    adminMetricsService.getTotalUserCount(),
                    postAdminService.countRecentPosts(AdminConstants.STATS_RECENT_DAYS));
        } catch (Exception e) {
            log.error("대시보드 통계 조회 실패", e);
            return new AdminStatsSummary(0, 0, 0, 0, 0);
        }
    }

    private AdminPendingItemsSummary buildPending() {
        try {
            return new AdminPendingItemsSummary(
                    adminPendingItemsService.getPendingCerts(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                    adminPendingItemsService.getPendingCertCount(),
                    adminPendingItemsService.getPendingReports(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                    adminPendingItemsService.getPendingReportCount(),
                    adminPendingItemsService.getPendingSongRequests(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                    adminPendingItemsService.getPendingSongRequestCount(),
                    artistSuggestionAdminService.getPendingSuggestionsPreview(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                    artistSuggestionAdminService.countPending());
        } catch (Exception e) {
            log.error("대시보드 처리대기 항목 조회 실패", e);
            return new AdminPendingItemsSummary(List.of(), 0, List.of(), 0, List.of(), 0, List.of(), 0);
        }
    }

    private AdminContentSummary buildContent() {
        try {
            return new AdminContentSummary(
                    postAdminService.getAdminHotPosts(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                    artistService.getTopArtists(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                    adminMetricsService.getRecentUsers(),
                    adminMetricsService.getDailyStats());
        } catch (Exception e) {
            log.error("대시보드 콘텐츠 조회 실패", e);
            return new AdminContentSummary(List.of(), List.of(), List.of(), List.of());
        }
    }
}
