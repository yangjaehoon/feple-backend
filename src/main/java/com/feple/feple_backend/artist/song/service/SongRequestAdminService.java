package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.entity.SongRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SongRequestAdminService {

    List<SongRequestResponseDto> getPendingRequests(Long artistId);

    /** 대시보드 미리보기용 — 아티스트 무관 전체 대기중 노래 신청을 최신순 최대 limit건 */
    List<SongRequest> getPendingPreview(int limit);

    Page<SongRequestResponseDto> getRequestsPage(int page, int size, String status, String keyword);

    long getPendingCount();

    boolean approveAndMaybeSaveSong(Long requestId, String youtubeUrl);

    void reject(Long requestId, String reason);
}
