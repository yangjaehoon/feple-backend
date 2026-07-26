package com.feple.feple_backend.artist.suggestion.service;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ArtistSuggestionAdminService {
    Page<ArtistSuggestionResponseDto> getSuggestionsPage(int page, int size);
    List<ArtistSuggestionResponseDto> getPendingSuggestionsPreview(int limit);
    List<ArtistSuggestionResponseDto> getProcessedSuggestionsPreview(int limit);
    long getPendingCount();
    long getProcessedCount();
    void approve(Long suggestionId, Long artistId);
    void dismiss(Long suggestionId, String processNote);
}
