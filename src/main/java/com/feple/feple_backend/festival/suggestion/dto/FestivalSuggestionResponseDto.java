package com.feple.feple_backend.festival.suggestion.dto;

import com.feple.feple_backend.festival.suggestion.entity.FestivalSuggestion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FestivalSuggestionResponseDto {
    private Long id;
    private String festivalName;
    private String note;
    private String processNote;
    private String status;
    private String userNickname;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public static FestivalSuggestionResponseDto from(FestivalSuggestion s, String nickname) {
        return FestivalSuggestionResponseDto.builder()
                .id(s.getId())
                .festivalName(s.getFestivalName())
                .note(s.getNote())
                .processNote(s.getProcessNote())
                .status(s.getStatus().name())
                .userNickname(nickname)
                .createdAt(s.getCreatedAt())
                .processedAt(s.getProcessedAt())
                .build();
    }
}
