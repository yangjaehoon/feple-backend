package com.feple.feple_backend.festival.suggestion.event;

public record FestivalSuggestionProcessedEvent(
        Long userId,
        Long festivalId,
        String festivalName,
        String note
) {}
