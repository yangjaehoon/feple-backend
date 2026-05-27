package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.dto.SubmitSongRequestDto;

import java.util.List;

public interface SongRequestService {

    SongRequestResponseDto submit(Long artistId, Long userId, SubmitSongRequestDto dto);

    List<SongRequestResponseDto> getMyRequests(Long artistId, Long userId);

    List<SongRequestResponseDto> getMyAllRequests(Long userId);
}
