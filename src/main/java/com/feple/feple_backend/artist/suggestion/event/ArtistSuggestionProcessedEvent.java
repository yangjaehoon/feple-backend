package com.feple.feple_backend.artist.suggestion.event;

public record ArtistSuggestionProcessedEvent(
        Long userId,
        String artistName,
        String note
) {}
