package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.service.ReportQueryService;
import com.feple.feple_backend.artist.song.service.SongRequestAdminService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import com.feple.feple_backend.festival.setlistchangerequest.service.SetlistChangeRequestService;
import com.feple.feple_backend.festival.suggestion.service.FestivalSuggestionAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminSidebarCountService {

    private final List<ReportQueryService<?>> reportServices;
    private final FestivalCertificationAdminService certificationService;
    private final SongRequestAdminService songRequestAdminService;
    private final ArtistSuggestionAdminService artistSuggestionAdminService;
    private final FestivalSuggestionAdminService festivalSuggestionAdminService;
    private final SetlistChangeRequestService setlistChangeRequestService;

    public record Counts(long reportCount, long certCount, long songRequestCount, long suggestionCount,
                          long festivalSuggestionCount, long setlistRequestCount) {}

    @Cacheable("adminSidebarCounts")
    public Counts getCounts() {
        return new Counts(
                reportServices.stream().mapToLong(ReportQueryService::getPendingCount).sum(),
                certificationService.getPendingCount(),
                songRequestAdminService.getPendingCount(),
                artistSuggestionAdminService.getPendingCount(),
                festivalSuggestionAdminService.getPendingCount(),
                setlistChangeRequestService.getPendingCount()
        );
    }
}
