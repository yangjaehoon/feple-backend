package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SongRequestAdminService {

    List<SongRequestResponseDto> getPendingRequests(Long artistId);

    Page<SongRequestResponseDto> getRequestsPage(int page, int size, String status);

    long getPendingCount();

    void approve(Long requestId, String youtubeUrl);

    void reject(Long requestId, String reason);
}
