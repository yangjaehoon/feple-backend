package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    @Mock PostReportRepository reportRepository;
    @Mock SongRequestRepository songRequestRepository;

    @InjectMocks AdminPendingItemsServiceImpl adminPendingItemsService;

    @Test
    void 대기중_인증_목록_limit만큼_반환() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(certificationRepository.findByStatusOrderByCreatedAtDesc(
                CertificationStatus.PENDING, PageRequest.of(0, 5)))
                .willReturn(new PageImpl<>(List.of(cert)));

        List<FestivalCertification> result = adminPendingItemsService.getPendingCerts(5);

        assertThat(result).containsExactly(cert);
    }

    @Test
    void 대기중_인증_건수_조회() {
        given(certificationRepository.countByStatus(CertificationStatus.PENDING)).willReturn(7L);

        long count = adminPendingItemsService.getPendingCertCount();

        assertThat(count).isEqualTo(7L);
        verify(certificationRepository).countByStatus(CertificationStatus.PENDING);
    }

    @Test
    void 대기중_신고_목록_limit만큼_반환() {
        PostReport report = mock(PostReport.class);
        given(reportRepository.findByStatusOrderByCreatedAtDesc(
                ReportStatus.PENDING, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(report)));

        List<PostReport> result = adminPendingItemsService.getPendingReports(10);

        assertThat(result).containsExactly(report);
    }

    @Test
    void 대기중_신고_건수_조회() {
        given(reportRepository.countByStatus(ReportStatus.PENDING)).willReturn(4L);

        long count = adminPendingItemsService.getPendingReportCount();

        assertThat(count).isEqualTo(4L);
        verify(reportRepository).countByStatus(ReportStatus.PENDING);
    }
}
