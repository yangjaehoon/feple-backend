package com.feple.feple_backend.artist.suggestion.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubmitArtistSuggestionDto {
    private String artistName;
    private String note;
}
