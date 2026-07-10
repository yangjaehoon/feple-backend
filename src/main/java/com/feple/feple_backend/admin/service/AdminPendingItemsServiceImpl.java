package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.certification.CertificationSummaryDto;
import com.feple.feple_backend.admin.moderation.PostReportSummaryDto;
import com.feple.feple_backend.admin.system.SongRequestSummaryDto;
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
    // 목록·카운트 모두 게시글 신고만 표시한다(PostReportSummaryDto가 게시글 전용이므로).
    // 사이드바 전체 신고 뱃지는 AdminSidebarCountService가 별도로 집계한다.
    private final PostReportRepository postReportRepository;
    private final SongRequestRepository songRequestRepository;

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'certs_' + #limit")
    public List<CertificationSummaryDto> getPendingCerts(int limit) {
        return certificationRepository
                .findByStatusOrderByCreatedAtDesc(CertificationStatus.PENDING, PageRequest.of(0, limit))
                .getContent()
                .stream().map(CertificationSummaryDto::from).toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'certCount'")
    public long getPendingCertCount() {
        return certificationRepository.countByStatus(CertificationStatus.PENDING);
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'reports_' + #limit")
    public List<PostReportSummaryDto> getPendingPostReports(int limit) {
        return postReportRepository
                .findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, PageRequest.of(0, limit))
                .getContent()
                .stream().map(PostReportSummaryDto::from).toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'reportCount'")
    public long getPendingPostReportCount() {
        return postReportRepository.countByStatus(ReportStatus.PENDING);
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
