package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.ContentTrendDto;
import com.feple.feple_backend.admin.DailyStatDto;
import com.feple.feple_backend.admin.UserActivityStatsDto;
import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.user.entity.User;

import java.util.List;

public interface AdminStatsService {
    long getTotalUserCount();
    List<User> getRecentUsers();
    List<DailyStatDto> getDailyStats();
    List<DailyStatDto> getMonthlyStats();
    UserActivityStatsDto getUserActivityStats();
    ContentTrendDto getContentTrend();
    List<FestivalCertification> getPendingCerts(int limit);
    long getPendingCertCount();
    List<PostReport> getPendingReports(int limit);
    long getPendingReportCount();
    List<SongRequest> getPendingSongRequests(int limit);
    long getPendingSongRequestCount();
}
