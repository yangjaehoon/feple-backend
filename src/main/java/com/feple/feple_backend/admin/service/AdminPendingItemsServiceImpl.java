package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.certification.CertSummaryDto;
import com.feple.feple_backend.admin.moderation.ReportSummaryDto;
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
    // 목록은 게시글 신고 전용 DTO(ReportSummaryDto)를 사용하므로 PostReportRepository로 직접 조회.
    // 카운트는 reportServices로 전 신고 유형(게시글·댓글·사진)을 합산해 사이드바 뱃지와 일치시킨다.
    private final PostReportRepository postReportRepository;
    private final List<ReportQueryService<?>> reportServices;
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
        return postReportRepository
                .findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, PageRequest.of(0, limit))
                .getContent()
                .stream().map(ReportSummaryDto::from).toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'reportCount'")
    public long getPendingReportCount() {
        return reportServices.stream().mapToLong(ReportQueryService::getPendingCount).sum();
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
