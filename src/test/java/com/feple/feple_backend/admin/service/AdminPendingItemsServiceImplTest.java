package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.certification.CertificationSummaryDto;
import com.feple.feple_backend.admin.moderation.PostReportSummaryDto;
import com.feple.feple_backend.artist.song.service.SongRequestAdminService;
import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionAdminService;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.service.PostReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminPendingItemsServiceImplTest {

    @Mock FestivalCertificationAdminService certificationService;
    @Mock PostReportService postReportService;
    @Mock SongRequestAdminService songRequestAdminService;
    @Mock ArtistSuggestionAdminService artistSuggestionAdminService;

    AdminPendingItemsServiceImpl adminPendingItemsService;

    @BeforeEach
    void setUp() {
        adminPendingItemsService = new AdminPendingItemsServiceImpl(
                certificationService,
                postReportService,
                songRequestAdminService,
                artistSuggestionAdminService
        );
    }

    @Test
    void 대기중_인증_목록_limit만큼_반환() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(certificationService.getPendingPreview(5)).willReturn(List.of(cert));

        List<CertificationSummaryDto> result = adminPendingItemsService.getPendingCerts(5);

        assertThat(result).hasSize(1);
    }

    @Test
    void 대기중_인증_건수_조회() {
        given(certificationService.getPendingCount()).willReturn(7L);

        long count = adminPendingItemsService.getPendingCertCount();

        assertThat(count).isEqualTo(7L);
        verify(certificationService).getPendingCount();
    }

    @Test
    void 대기중_신고_목록_게시글_신고만_반환() {
        PostReport report = mock(PostReport.class);
        given(postReportService.findPendingReports(PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(report)));

        List<PostReportSummaryDto> result = adminPendingItemsService.getPendingPostReports(10);

        assertThat(result).hasSize(1);
    }

    @Test
    void 대기중_신고_건수_게시글_신고만_집계() {
        given(postReportService.getPendingCount()).willReturn(5L);

        long count = adminPendingItemsService.getPendingPostReportCount();

        assertThat(count).isEqualTo(5L);
        verify(postReportService).getPendingCount();
    }

    @Test
    void 대기중_아티스트_신청_목록_limit만큼_반환() {
        ArtistSuggestionResponseDto suggestion = mock(ArtistSuggestionResponseDto.class);
        given(artistSuggestionAdminService.getPendingSuggestionsPreview(5)).willReturn(List.of(suggestion));

        List<ArtistSuggestionResponseDto> result = adminPendingItemsService.getPendingArtistSuggestions(5);

        assertThat(result).hasSize(1);
    }

    @Test
    void 대기중_아티스트_신청_건수_조회() {
        given(artistSuggestionAdminService.getPendingCount()).willReturn(3L);

        long count = adminPendingItemsService.getPendingArtistSuggestionCount();

        assertThat(count).isEqualTo(3L);
        verify(artistSuggestionAdminService).getPendingCount();
    }
}
