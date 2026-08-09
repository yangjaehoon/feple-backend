package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;

import java.util.List;

public interface ArtistService {
    /** weeklyScore(주간 랭킹) 기준 상위 아티스트 목록 — 전체 아티스트가 아니라 상한(PageSize.ARTIST_RANKING)까지만 반환한다. */
    List<ArtistResponseDto> getArtistRanking();
    ArtistResponseDto getArtistById(Long id);
    List<ArtistResponseDto> searchArtists(String keyword);
    List<ArtistResponseDto> getFollowedArtists(Long userId);
    List<ArtistResponseDto> getRelatedArtists(Long artistId, int limit);
}
