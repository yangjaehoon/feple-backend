package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.service.AdminMetricsService;
import com.feple.feple_backend.admin.service.AdminPendingItemsService;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.post.service.PostAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardAssembler {

    private final FestivalService festivalService;
    private final ArtistService artistService;
    private final PostAdminService postAdminService;
    private final AdminMetricsService adminMetricsService;
    private final AdminPendingItemsService adminPendingItemsService;
    private final ArtistSuggestionAdminService artistSuggestionAdminService;

    public AdminDashboardDto assemble() {
        return new AdminDashboardDto(
                festivalService.getTotalCount(),
                artistService.getTotalCount(),
                postAdminService.getTotalPostCount(),
                adminMetricsService.getTotalUserCount(),
                postAdminService.countRecentPosts(AdminConstants.STATS_RECENT_DAYS),
                postAdminService.getAdminHotPosts(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                artistService.getTopArtists(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                adminMetricsService.getRecentUsers(),
                adminMetricsService.getDailyStats(),
                adminPendingItemsService.getPendingCerts(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                adminPendingItemsService.getPendingCertCount(),
                adminPendingItemsService.getPendingReports(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                adminPendingItemsService.getPendingReportCount(),
                adminPendingItemsService.getPendingSongRequests(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                adminPendingItemsService.getPendingSongRequestCount(),
                artistSuggestionAdminService.getPendingSuggestionsPreview(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                artistSuggestionAdminService.countPending()
        );
    }
}
