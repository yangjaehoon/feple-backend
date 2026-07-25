package com.feple.feple_backend.festival.suggestion.service;

import com.feple.feple_backend.festival.suggestion.dto.FestivalSuggestionResponseDto;
import com.feple.feple_backend.festival.suggestion.dto.SubmitFestivalSuggestionDto;

public interface FestivalSuggestionService {
    FestivalSuggestionResponseDto submit(Long userId, SubmitFestivalSuggestionDto dto);
    void removeAllByUser(Long userId);
}
