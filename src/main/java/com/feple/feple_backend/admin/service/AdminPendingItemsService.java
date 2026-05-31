package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.post.entity.PostReport;

import java.util.List;

public interface AdminPendingItemsService {
    List<FestivalCertification> getPendingCerts(int limit);
    long getPendingCertCount();
    List<PostReport> getPendingReports(int limit);
    long getPendingReportCount();
    List<SongRequest> getPendingSongRequests(int limit);
    long getPendingSongRequestCount();
}
