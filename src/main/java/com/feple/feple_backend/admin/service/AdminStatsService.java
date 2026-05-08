package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.DailyStatDto;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.user.entity.User;

import java.util.List;

public interface AdminStatsService {
    long getTotalUserCount();
    List<User> getRecentUsers();
    List<DailyStatDto> getDailyStats();
    List<FestivalCertification> getPendingCerts(int limit);
    long getPendingCertCount();
    List<PostReport> getPendingReports(int limit);
    long getPendingReportCount();
}
