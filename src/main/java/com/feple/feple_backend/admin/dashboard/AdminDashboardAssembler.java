package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.certification.CertificationSummaryDto;
import com.feple.feple_backend.admin.moderation.PostReportSummaryDto;
import com.feple.feple_backend.admin.service.AdminDashboardMetrics;
import com.feple.feple_backend.admin.service.AdminPendingItemsService;
import com.feple.feple_backend.admin.system.SongRequestSummaryDto;
import com.feple.feple_backend.admin.user.UserSummaryDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardAssembler {

    private final FestivalAdminService festivalService;
    private final ArtistAdminService artistService;
    private final PostAdminService postAdminService;
    private final AdminDashboardMetrics adminMetricsService;
    private final AdminPendingItemsService adminPendingItemsService;
    private final Executor dashboardExecutor;

    public AdminDashboardDto assemble() {
        return new AdminDashboardDto(buildStats(), buildPending(), buildContent());
    }

    // 통계·처리대기·콘텐츠를 섹션별로 격리해 하나가 실패해도 나머지 섹션은 렌더링된다.
    // 섹션 내부 조회들은 서로 의존성이 없으므로 dashboardExecutor로 병렬 실행 후 join한다.

    private <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, dashboardExecutor);
    }

    private AdminStatsSummary buildStats() {
        try {
            CompletableFuture<Long> totalFestivals = async(festivalService::getTotalCount);
            CompletableFuture<Long> totalArtists = async(artistService::getTotalCount);
            CompletableFuture<Long> totalPosts = async(postAdminService::getTotalPostCount);
            CompletableFuture<Long> totalUsers = async(adminMetricsService::getTotalUserCount);
            CompletableFuture<Long> recentPosts = async(() -> postAdminService.countRecentPosts(AdminConstants.STATS_RECENT_DAYS));

            return new AdminStatsSummary(
                    totalFestivals.join(), totalArtists.join(), totalPosts.join(),
                    totalUsers.join(), recentPosts.join());
        } catch (Exception e) {
            log.error("대시보드 통계 조회 실패", e);
            return new AdminStatsSummary(0, 0, 0, 0, 0);
        }
    }

    private AdminPendingItemsSummary buildPending() {
        try {
            CompletableFuture<List<CertificationSummaryDto>> certs = async(() -> adminPendingItemsService.getPendingCerts(AdminConstants.DASHBOARD_PREVIEW_SIZE));
            CompletableFuture<Long> certCount = async(adminPendingItemsService::getPendingCertCount);
            CompletableFuture<List<PostReportSummaryDto>> reports = async(() -> adminPendingItemsService.getPendingPostReports(AdminConstants.DASHBOARD_PREVIEW_SIZE));
            CompletableFuture<Long> reportCount = async(adminPendingItemsService::getPendingPostReportCount);
            CompletableFuture<List<SongRequestSummaryDto>> songRequests = async(() -> adminPendingItemsService.getPendingSongRequests(AdminConstants.DASHBOARD_PREVIEW_SIZE));
            CompletableFuture<Long> songRequestCount = async(adminPendingItemsService::getPendingSongRequestCount);
            CompletableFuture<List<ArtistSuggestionResponseDto>> artistSuggestions = async(() -> adminPendingItemsService.getPendingArtistSuggestions(AdminConstants.DASHBOARD_PREVIEW_SIZE));
            CompletableFuture<Long> artistSuggestionCount = async(adminPendingItemsService::getPendingArtistSuggestionCount);

            return new AdminPendingItemsSummary(
                    certs.join(), certCount.join(),
                    reports.join(), reportCount.join(),
                    songRequests.join(), songRequestCount.join(),
                    artistSuggestions.join(), artistSuggestionCount.join());
        } catch (Exception e) {
            log.error("대시보드 처리대기 항목 조회 실패", e);
            return new AdminPendingItemsSummary(List.of(), 0, List.of(), 0, List.of(), 0, List.of(), 0);
        }
    }

    private AdminContentSummary buildContent() {
        try {
            CompletableFuture<List<PostResponseDto>> hotPosts = async(() -> postAdminService.getAdminHotPosts(AdminConstants.DASHBOARD_PREVIEW_SIZE));
            CompletableFuture<List<ArtistResponseDto>> topArtists = async(() -> artistService.getTopArtists(AdminConstants.DASHBOARD_PREVIEW_SIZE));
            CompletableFuture<List<UserSummaryDto>> recentUsers = async(adminMetricsService::getRecentUsers);
            CompletableFuture<List<DailyStatDto>> dailyStats = async(adminMetricsService::getDailyStats);

            return new AdminContentSummary(
                    hotPosts.join(), topArtists.join(), recentUsers.join(), dailyStats.join());
        } catch (Exception e) {
            log.error("대시보드 콘텐츠 조회 실패", e);
            return new AdminContentSummary(List.of(), List.of(), List.of(), List.of());
        }
    }
}
