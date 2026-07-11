package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.certification.CertificationSummaryDto;
import com.feple.feple_backend.admin.moderation.PostReportSummaryDto;
import com.feple.feple_backend.admin.system.SongRequestSummaryDto;
import com.feple.feple_backend.artist.song.service.SongRequestAdminService;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import com.feple.feple_backend.post.service.PostReportService;
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

    private final FestivalCertificationAdminService certificationService;
    // 목록·카운트 모두 게시글 신고만 표시한다(PostReportSummaryDto가 게시글 전용이므로).
    // 사이드바 전체 신고 뱃지는 AdminSidebarCountService가 별도로 집계한다.
    private final PostReportService postReportService;
    private final SongRequestAdminService songRequestAdminService;

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'certs_' + #limit")
    public List<CertificationSummaryDto> getPendingCerts(int limit) {
        return certificationService.getPendingPreview(limit)
                .stream().map(CertificationSummaryDto::from).toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'certCount'")
    public long getPendingCertCount() {
        return certificationService.getPendingCount();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'reports_' + #limit")
    public List<PostReportSummaryDto> getPendingPostReports(int limit) {
        return postReportService.findPendingReports(PageRequest.of(0, limit))
                .getContent()
                .stream().map(PostReportSummaryDto::from).toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'reportCount'")
    public long getPendingPostReportCount() {
        return postReportService.getPendingCount();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'songs_' + #limit")
    public List<SongRequestSummaryDto> getPendingSongRequests(int limit) {
        return songRequestAdminService.getPendingPreview(limit)
                .stream().map(SongRequestSummaryDto::from).toList();
    }

    @Override
    @Cacheable(value = "adminPendingCounts", key = "'songCount'")
    public long getPendingSongRequestCount() {
        return songRequestAdminService.getPendingCount();
    }
}
