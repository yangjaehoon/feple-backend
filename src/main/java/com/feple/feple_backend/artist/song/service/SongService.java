package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.song.dto.SongResponseDto;

import java.util.List;

public interface SongService {
    List<SongResponseDto> getSongsByArtistId(Long artistId);
}
