package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;

import java.util.List;

public interface ArtistService {
    List<ArtistResponseDto> getAllArtists();
    ArtistResponseDto getArtistById(Long id);
    List<ArtistResponseDto> searchArtists(String keyword);
    List<ArtistResponseDto> getFollowedArtists(Long userId);
    List<ArtistResponseDto> getRelatedArtists(Long artistId, int limit);
}
