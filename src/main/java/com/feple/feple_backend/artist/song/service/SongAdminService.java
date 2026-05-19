package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.song.dto.SaveSongRequestDto;
import com.feple.feple_backend.artist.song.dto.SongResponseDto;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;

import java.util.List;

public interface SongAdminService {
    List<YoutubeVideoDto> searchYoutube(String query);
    SongResponseDto saveSong(Long artistId, SaveSongRequestDto dto);
    void deleteSong(Long songId);
}
