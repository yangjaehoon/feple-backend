package com.feple.feple_backend.festival.suggestion.service;

import com.feple.feple_backend.festival.suggestion.dto.FestivalSuggestionResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface FestivalSuggestionAdminService {
    Page<FestivalSuggestionResponseDto> getSuggestionsPage(int page, int size);
    List<FestivalSuggestionResponseDto> getPendingSuggestionsPreview(int limit);
    List<FestivalSuggestionResponseDto> getProcessedSuggestionsPreview(int limit);
    long getPendingCount();
    long getProcessedCount();
    void approve(Long suggestionId, Long festivalId);
    void dismiss(Long suggestionId, String processNote);
}
