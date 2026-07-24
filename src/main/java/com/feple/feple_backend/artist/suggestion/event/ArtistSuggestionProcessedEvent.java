package com.feple.feple_backend.artist.suggestion.event;

public record ArtistSuggestionProcessedEvent(
        Long userId,
        Long artistId,
        String artistName,
        String note
) {}
