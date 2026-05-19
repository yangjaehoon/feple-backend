package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;

import java.util.List;

public interface SongRequestAdminService {

    List<SongRequestResponseDto> getPendingRequests(Long artistId);

    void approve(Long requestId, String youtubeUrl);

    void reject(Long requestId);
}
