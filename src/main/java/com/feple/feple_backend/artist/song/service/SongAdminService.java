package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.song.dto.SaveSongDto;
import com.feple.feple_backend.artist.song.dto.SongResponseDto;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;

import java.util.List;
import java.util.Optional;

public interface SongAdminService {
    List<YoutubeVideoDto> searchYoutube(String artistName, String query);
    Optional<YoutubeVideoDto> fetchVideoByUrl(String videoUrlOrId);
    SongResponseDto saveSong(Long artistId, SaveSongDto dto);
    void deleteSong(Long artistId, Long songId);
}
