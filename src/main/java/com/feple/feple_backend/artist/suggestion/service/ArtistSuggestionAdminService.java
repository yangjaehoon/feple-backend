package com.feple.feple_backend.artist.suggestion.service;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;

import java.util.List;

public interface ArtistSuggestionAdminService {
    List<ArtistSuggestionResponseDto> getPendingSuggestions();
    long countPending();
    void dismiss(Long suggestionId);
}
