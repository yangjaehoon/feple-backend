package com.feple.feple_backend.artist.suggestion.service;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.dto.SubmitArtistSuggestionDto;

public interface ArtistSuggestionService {
    ArtistSuggestionResponseDto submit(Long userId, SubmitArtistSuggestionDto dto);
}
