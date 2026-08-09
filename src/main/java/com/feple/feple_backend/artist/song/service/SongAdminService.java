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
    /** saveSong과 달리 이미 등록된 곡이어도 예외를 던지지 않고 빈 값을 반환한다 — 곡 신청 승인처럼
     *  중복이 실패로 취급되면 안 되는 호출부(SongRequestServiceImpl)를 위한 것. */
    Optional<SongResponseDto> saveSongIfAbsent(Long artistId, SaveSongDto dto);
    void deleteSong(Long artistId, Long songId);
}
