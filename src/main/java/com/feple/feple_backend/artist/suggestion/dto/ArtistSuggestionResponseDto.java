package com.feple.feple_backend.artist.suggestion.dto;

import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ArtistSuggestionResponseDto {
    private Long id;
    private String artistName;
    private String note;
    private String processNote;
    private String status;
    private String userNickname;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public static ArtistSuggestionResponseDto from(ArtistSuggestion s, String nickname) {
        return ArtistSuggestionResponseDto.builder()
                .id(s.getId())
                .artistName(s.getArtistName())
                .note(s.getNote())
                .processNote(s.getProcessNote())
                .status(s.getStatus().name())
                .userNickname(nickname)
                .createdAt(s.getCreatedAt())
                .processedAt(s.getProcessedAt())
                .build();
    }
}
