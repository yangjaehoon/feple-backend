package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.certification.CertificationSummaryDto;
import com.feple.feple_backend.admin.moderation.PostReportSummaryDto;
import com.feple.feple_backend.admin.system.SongRequestSummaryDto;
import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;

import java.util.List;

public interface AdminPendingItemsService {
    List<CertificationSummaryDto> getPendingCerts(int limit);
    long getPendingCertCount();
    List<PostReportSummaryDto> getPendingPostReports(int limit);
    long getPendingPostReportCount();
    List<SongRequestSummaryDto> getPendingSongRequests(int limit);
    long getPendingSongRequestCount();
    List<ArtistSuggestionResponseDto> getPendingArtistSuggestions(int limit);
    long getPendingArtistSuggestionCount();
}
