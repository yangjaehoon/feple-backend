package com.feple.feple_backend.artist.suggestion.service;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ArtistSuggestionAdminService {
    Page<ArtistSuggestionResponseDto> getSuggestionsPage(int page, int size);
    List<ArtistSuggestionResponseDto> getPendingSuggestionsPreview(int limit);
    List<ArtistSuggestionResponseDto> getProcessedSuggestionsPreview(int limit);
    long getPendingCount();
    long getProcessedCount();
    void approve(Long suggestionId, Long artistId);
    void dismiss(Long suggestionId, String processNote);
}
