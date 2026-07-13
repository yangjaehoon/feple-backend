package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.service.ReportQueryService;
import com.feple.feple_backend.artist.song.service.SongRequestAdminService;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import com.feple.feple_backend.festival.setlistchangerequest.service.SetlistChangeRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminSidebarCountServiceTest {

    @Mock ReportQueryService<?> postReportService;
    @Mock ReportQueryService<?> commentReportService;
    @Mock FestivalCertificationAdminService certificationService;
    @Mock SongRequestAdminService songRequestAdminService;
    @Mock ArtistSuggestionAdminService artistSuggestionAdminService;
    @Mock SetlistChangeRequestService setlistChangeRequestService;

    AdminSidebarCountService sidebarCountService;

    @BeforeEach
    void setUp() {
        sidebarCountService = new AdminSidebarCountService(
                List.of(postReportService, commentReportService),
                certificationService,
                songRequestAdminService,
                artistSuggestionAdminService,
                setlistChangeRequestService
        );
    }

    @Test
    void getCounts_여러_신고_서비스의_pendingCount를_합산() {
        given(postReportService.getPendingCount()).willReturn(3L);
        given(commentReportService.getPendingCount()).willReturn(2L);
        given(certificationService.getPendingCount()).willReturn(5L);
        given(songRequestAdminService.getPendingCount()).willReturn(1L);
        given(artistSuggestionAdminService.getPendingCount()).willReturn(4L);
        given(setlistChangeRequestService.getPendingCount()).willReturn(7L);

        AdminSidebarCountService.Counts counts = sidebarCountService.getCounts();

        assertThat(counts.reportCount()).isEqualTo(5L);
        assertThat(counts.certCount()).isEqualTo(5L);
        assertThat(counts.songRequestCount()).isEqualTo(1L);
        assertThat(counts.suggestionCount()).isEqualTo(4L);
        assertThat(counts.setlistRequestCount()).isEqualTo(7L);
    }

    @Test
    void getCounts_신고_서비스가_하나만_있을_때() {
        sidebarCountService = new AdminSidebarCountService(
                List.of(postReportService),
                certificationService,
                songRequestAdminService,
                artistSuggestionAdminService,
                setlistChangeRequestService
        );
        given(postReportService.getPendingCount()).willReturn(10L);
        given(certificationService.getPendingCount()).willReturn(0L);
        given(songRequestAdminService.getPendingCount()).willReturn(0L);
        given(artistSuggestionAdminService.getPendingCount()).willReturn(0L);
        given(setlistChangeRequestService.getPendingCount()).willReturn(0L);

        AdminSidebarCountService.Counts counts = sidebarCountService.getCounts();

        assertThat(counts.reportCount()).isEqualTo(10L);
    }

    @Test
    void getCounts_모두_0이면_0_반환() {
        given(postReportService.getPendingCount()).willReturn(0L);
        given(commentReportService.getPendingCount()).willReturn(0L);
        given(certificationService.getPendingCount()).willReturn(0L);
        given(songRequestAdminService.getPendingCount()).willReturn(0L);
        given(artistSuggestionAdminService.getPendingCount()).willReturn(0L);
        given(setlistChangeRequestService.getPendingCount()).willReturn(0L);

        AdminSidebarCountService.Counts counts = sidebarCountService.getCounts();

        assertThat(counts.reportCount()).isZero();
        assertThat(counts.certCount()).isZero();
        assertThat(counts.songRequestCount()).isZero();
        assertThat(counts.suggestionCount()).isZero();
        assertThat(counts.setlistRequestCount()).isZero();
    }
}
