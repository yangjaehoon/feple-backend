package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostReportRepository;
import lombok.RequiredArgsConstructor;
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
    public List<FestivalCertification> getPendingCerts(int limit) {
        return certificationRepository
                .findByStatusOrderByCreatedAtDesc(CertificationStatus.PENDING, PageRequest.of(0, limit))
                .getContent();
    }

    @Override
    public long getPendingCertCount() {
        return certificationRepository.countByStatus(CertificationStatus.PENDING);
    }

    @Override
    public List<PostReport> getPendingReports(int limit) {
        return reportRepository
                .findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, PageRequest.of(0, limit))
                .getContent();
    }

    @Override
    public long getPendingReportCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Override
    public List<SongRequest> getPendingSongRequests(int limit) {
        return songRequestRepository.findByStatusOrderByCreatedAtDesc(
                SongRequestStatus.PENDING, PageRequest.of(0, limit));
    }

    @Override
    public long getPendingSongRequestCount() {
        return songRequestRepository.countByStatus(SongRequestStatus.PENDING);
    }
}
