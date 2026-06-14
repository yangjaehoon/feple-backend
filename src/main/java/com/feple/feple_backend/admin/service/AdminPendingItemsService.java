package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.CertSummaryDto;
import com.feple.feple_backend.admin.ReportSummaryDto;
import com.feple.feple_backend.admin.SongRequestSummaryDto;

import java.util.List;

public interface AdminPendingItemsService {
    List<CertSummaryDto> getPendingCerts(int limit);
    long getPendingCertCount();
    List<ReportSummaryDto> getPendingReports(int limit);
    long getPendingReportCount();
    List<SongRequestSummaryDto> getPendingSongRequests(int limit);
    long getPendingSongRequestCount();
}
