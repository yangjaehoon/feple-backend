package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.service.ReportQueryService;
import com.feple.feple_backend.artist.song.service.SongRequestAdminService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice(basePackages = "com.feple.feple_backend.admin")
@RequiredArgsConstructor
public class AdminSidebarAdvice {

    private final List<ReportQueryService> reportServices;
    private final FestivalCertificationService certificationService;
    private final SongRequestAdminService songRequestAdminService;
    private final ArtistSuggestionAdminService artistSuggestionAdminService;

    @ModelAttribute("sidebarReportCount")
    public long sidebarReportCount() {
        try {
            return reportServices.stream().mapToLong(ReportQueryService::getPendingCount).sum();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarCertCount")
    public long sidebarCertCount() {
        try {
            return certificationService.getPendingCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarSongRequestCount")
    public long sidebarSongRequestCount() {
        try {
            return songRequestAdminService.getPendingCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarArtistSuggestionCount")
    public long sidebarArtistSuggestionCount() {
        try {
            return artistSuggestionAdminService.countPending();
        } catch (Exception e) {
            return 0;
        }
    }
}
