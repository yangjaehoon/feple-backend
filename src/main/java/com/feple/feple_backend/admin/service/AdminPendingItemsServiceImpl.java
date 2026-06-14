package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.CertSummaryDto;
import com.feple.feple_backend.admin.ReportSummaryDto;
import com.feple.feple_backend.admin.SongRequestSummaryDto;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPendingItemsServiceImpl implements AdminPendingItemsService {

    private final FestivalCertificationRepository certificationRepository;
    private final PostReportRepository reportRepository;
    private final SongRequestRepository songRequestRepository;

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'certs_' + #limit")
    public List<CertSummaryDto> getPendingCerts(int limit) {
        return certificationRepository
                .findByStatusOrderByCreatedAtDesc(CertificationStatus.PENDING, PageRequest.of(0, limit))
                .getContent()
                .stream().map(CertSummaryDto::from).toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'certCount'")
    public long getPendingCertCount() {
        return certificationRepository.countByStatus(CertificationStatus.PENDING);
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'reports_' + #limit")
    public List<ReportSummaryDto> getPendingReports(int limit) {
        return reportRepository
                .findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, PageRequest.of(0, limit))
                .getContent()
                .stream().map(ReportSummaryDto::from).toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'reportCount'")
    public long getPendingReportCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'songs_' + #limit")
    public List<SongRequestSummaryDto> getPendingSongRequests(int limit) {
        return songRequestRepository.findByStatusOrderByCreatedAtDesc(
                        SongRequestStatus.PENDING, PageRequest.of(0, limit))
                .stream().map(SongRequestSummaryDto::from).toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'songCount'")
    public long getPendingSongRequestCount() {
        return songRequestRepository.countByStatus(SongRequestStatus.PENDING);
    }
}
