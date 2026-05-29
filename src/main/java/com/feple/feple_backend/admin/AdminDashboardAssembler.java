package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.service.AdminStatsService;
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
    private final AdminStatsService adminStatsService;
    private final ArtistSuggestionAdminService artistSuggestionAdminService;

    public AdminDashboardDto assemble(int festivalPage, int artistPage) {
        return new AdminDashboardDto(
                festivalService.getFestivalsPage(festivalPage, AdminConstants.DASHBOARD_PAGE_SIZE),
                artistService.getArtistsPage(artistPage, AdminConstants.DASHBOARD_PAGE_SIZE),
                postAdminService.getTotalPostCount(),
                adminStatsService.getTotalUserCount(),
                postAdminService.countRecentPosts(AdminConstants.STATS_RECENT_DAYS),
                postAdminService.getAdminHotPosts(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                artistService.getTopArtists(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                adminStatsService.getRecentUsers(),
                adminStatsService.getDailyStats(),
                adminStatsService.getPendingCerts(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                adminStatsService.getPendingCertCount(),
                adminStatsService.getPendingReports(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                adminStatsService.getPendingReportCount(),
                adminStatsService.getPendingSongRequests(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                adminStatsService.getPendingSongRequestCount(),
                artistSuggestionAdminService.getPendingSuggestionsPreview(AdminConstants.DASHBOARD_PREVIEW_SIZE),
                artistSuggestionAdminService.countPending()
        );
    }
}
