package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.admin.certification.CertificationSummaryDto;
import com.feple.feple_backend.admin.moderation.ReportSummaryDto;
import com.feple.feple_backend.admin.system.SongRequestSummaryDto;
import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;

import java.util.List;

public record AdminPendingItemsSummary(
        List<CertificationSummaryDto> certs,
        long certCount,
        List<ReportSummaryDto> reports,
        long reportCount,
        List<SongRequestSummaryDto> songRequests,
        long songRequestCount,
        List<ArtistSuggestionResponseDto> artistSuggestions,
        long artistSuggestionCount
) {}
