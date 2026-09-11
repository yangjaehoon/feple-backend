package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.admin.certification.CertificationSummaryDto;
import com.feple.feple_backend.admin.moderation.PostReportSummaryDto;
import com.feple.feple_backend.admin.service.AdminDashboardMetrics;
import com.feple.feple_backend.admin.service.AdminPendingItemsService;
import com.feple.feple_backend.admin.support.AdminConstants;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostAdminService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        SectionResult<AdminStatsSummary> stats = buildStats();
        SectionResult<AdminPendingItemsSummary> pending = buildPending();
        SectionResult<AdminContentSummary> content = buildContent();
        boolean hasLoadError = stats.failed() || pending.failed() || content.failed();
        return new AdminDashboardDto(stats.value(), pending.value(), content.value(), hasLoadError);
    }

    // 통계·처리대기·콘텐츠를 섹션별로 격리해 하나가 실패해도 나머지 섹션은 렌더링된다.
    // 섹션 내부 조회들은 서로 의존성이 없으므로 dashboardExecutor로 병렬 실행 후 join한다.

    private <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, dashboardExecutor);
    }

    private record SectionResult<T>(T value, boolean failed) {}

    // 세 build* 메서드가 공유하는 "실패 시 로그 남기고 폴백으로 대체" 패턴.
    private <T> SectionResult<T> loadSection(String errorLogMessage, Supplier<T> loader, T fallback) {
        try {
            return new SectionResult<>(loader.get(), false);
        } catch (Exception e) {
            log.error(errorLogMessage, e);
            return new SectionResult<>(fallback, true);
        }
    }

    private SectionResult<AdminStatsSummary> buildStats() {
        return loadSection("대시보드 통계 조회 실패", () -> {
            CompletableFuture<Long> totalFestivals = async(festivalService::getTotalCount);
            CompletableFuture<Long> totalArtists = async(artistService::getTotalCount);
            CompletableFuture<Long> totalPosts = async(postAdminService::getTotalPostCount);
            CompletableFuture<Long> totalUsers = async(adminMetricsService::getTotalUserCount);
            CompletableFuture<Long> recentPosts = async(() -> postAdminService.countRecentPosts(AdminConstants.STATS_RECENT_DAYS));

            return new AdminStatsSummary(
                    totalFestivals.join(), totalArtists.join(), totalPosts.join(),
                    totalUsers.join(), recentPosts.join());
        }, new AdminStatsSummary(0, 0, 0, 0, 0));
    }

    private SectionResult<AdminPendingItemsSummary> buildPending() {
        return loadSection("대시보드 처리대기 항목 조회 실패", () -> {
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
        }, new AdminPendingItemsSummary(List.of(), 0, List.of(), 0, List.of(), 0, List.of(), 0));
    }

    private SectionResult<AdminContentSummary> buildContent() {
        return loadSection("대시보드 콘텐츠 조회 실패", () -> {
            CompletableFuture<List<PostResponseDto>> hotPosts = async(() -> postAdminService.getAdminHotPosts(AdminConstants.DASHBOARD_PREVIEW_SIZE));
            CompletableFuture<List<ArtistResponseDto>> topArtists = async(() -> artistService.getTopArtists(AdminConstants.DASHBOARD_PREVIEW_SIZE));
            CompletableFuture<List<UserSummaryDto>> recentUsers = async(adminMetricsService::getRecentUsers);
            CompletableFuture<List<DailyStatDto>> dailyStats = async(adminMetricsService::getDailyStats);

            return new AdminContentSummary(
                    hotPosts.join(), topArtists.join(), recentUsers.join(), dailyStats.join());
        }, new AdminContentSummary(List.of(), List.of(), List.of(), List.of()));
    }
}
