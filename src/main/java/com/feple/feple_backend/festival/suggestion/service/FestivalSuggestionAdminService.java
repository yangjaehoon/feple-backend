package com.feple.feple_backend.festival.suggestion.service;

import com.feple.feple_backend.festival.suggestion.dto.FestivalSuggestionResponseDto;
import java.util.List;
import org.springframework.data.domain.Page;

public interface FestivalSuggestionAdminService {
    Page<FestivalSuggestionResponseDto> getSuggestionsPage(int page, int size);
    List<FestivalSuggestionResponseDto> getPendingSuggestionsPreview(int limit);
    List<FestivalSuggestionResponseDto> getProcessedSuggestionsPreview(int limit);
    long getPendingCount();
    long getProcessedCount();
    void approve(Long suggestionId, Long festivalId);
    void dismiss(Long suggestionId, String processNote);
}
