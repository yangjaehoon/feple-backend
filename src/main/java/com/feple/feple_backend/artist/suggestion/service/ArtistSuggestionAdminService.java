package com.feple.feple_backend.artist.suggestion.service;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ArtistSuggestionAdminService {
    List<ArtistSuggestionResponseDto> getPendingSuggestions();

    Page<ArtistSuggestionResponseDto> getSuggestionsPage(int page, int size);
    List<ArtistSuggestionResponseDto> getPendingSuggestionsPreview(int limit);
    List<ArtistSuggestionResponseDto> getProcessedSuggestions();
    long countPending();
    void dismiss(Long suggestionId, String processNote);
}
