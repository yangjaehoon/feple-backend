package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.song.dto.SaveSongRequestDto;
import com.feple.feple_backend.artist.song.dto.SongResponseDto;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import com.feple.feple_backend.artist.song.entity.ArtistFestivalSong;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SongAdminService {
    List<YoutubeVideoDto> searchYoutube(String artistName, String query);
    Optional<YoutubeVideoDto> fetchVideoByUrl(String videoUrlOrId);
    SongResponseDto saveSong(Long artistId, SaveSongRequestDto dto);
    void deleteSong(Long songId);

    List<ArtistFestivalSong> getSetlist(Long artistFestivalId);
    void saveSetlist(Long artistFestivalId, Set<Long> songIds);
}
