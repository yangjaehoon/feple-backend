package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.certification.CertSummaryDto;
import com.feple.feple_backend.admin.moderation.ReportSummaryDto;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostReportRepository;
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

    @Mock FestivalCertificationRepository certificationRepository;
    @Mock PostReportRepository postReportRepository;
    @Mock SongRequestRepository songRequestRepository;

    AdminPendingItemsServiceImpl adminPendingItemsService;

    @BeforeEach
    void setUp() {
        adminPendingItemsService = new AdminPendingItemsServiceImpl(
                certificationRepository,
                postReportRepository,
                songRequestRepository
        );
    }

    @Test
    void 대기중_인증_목록_limit만큼_반환() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(certificationRepository.findByStatusOrderByCreatedAtDesc(
                CertificationStatus.PENDING, PageRequest.of(0, 5)))
                .willReturn(new PageImpl<>(List.of(cert)));

        List<CertSummaryDto> result = adminPendingItemsService.getPendingCerts(5);

        assertThat(result).hasSize(1);
    }

    @Test
    void 대기중_인증_건수_조회() {
        given(certificationRepository.countByStatus(CertificationStatus.PENDING)).willReturn(7L);

        long count = adminPendingItemsService.getPendingCertCount();

        assertThat(count).isEqualTo(7L);
        verify(certificationRepository).countByStatus(CertificationStatus.PENDING);
    }

    @Test
    void 대기중_신고_목록_게시글_신고만_반환() {
        PostReport report = mock(PostReport.class);
        given(postReportRepository.findByStatusOrderByCreatedAtDesc(
                ReportStatus.PENDING, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(report)));

        List<ReportSummaryDto> result = adminPendingItemsService.getPendingReports(10);

        assertThat(result).hasSize(1);
    }

    @Test
    void 대기중_신고_건수_게시글_신고만_집계() {
        given(postReportRepository.countByStatus(ReportStatus.PENDING)).willReturn(5L);

        long count = adminPendingItemsService.getPendingReportCount();

        assertThat(count).isEqualTo(5L);
        verify(postReportRepository).countByStatus(ReportStatus.PENDING);
    }
}
