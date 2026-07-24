package com.feple.feple_backend.admin.ocr;

public record UnmatchedArtistSuggestionDto(Long id, String name, int mentionCount) {

    public static UnmatchedArtistSuggestionDto from(UnmatchedArtistSuggestion s) {
        return new UnmatchedArtistSuggestionDto(s.getId(), s.getName(), s.getMentionCount());
    }
}
