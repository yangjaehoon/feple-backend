package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.certification.CertificationSummaryDto;
import com.feple.feple_backend.admin.moderation.ReportSummaryDto;
import com.feple.feple_backend.admin.system.SongRequestSummaryDto;

import java.util.List;

public interface AdminPendingItemsService {
    List<CertificationSummaryDto> getPendingCerts(int limit);
    long getPendingCertCount();
    List<ReportSummaryDto> getPendingReports(int limit);
    long getPendingReportCount();
    List<SongRequestSummaryDto> getPendingSongRequests(int limit);
    long getPendingSongRequestCount();
}
