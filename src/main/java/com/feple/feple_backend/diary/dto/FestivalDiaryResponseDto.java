package com.feple.feple_backend.diary.dto;

import com.feple.feple_backend.diary.entity.DiaryVisibility;
import com.feple.feple_backend.diary.entity.FestivalDiary;
import java.time.LocalDateTime;
import java.util.List;

public record FestivalDiaryResponseDto(
        Long id,
        Long festivalId,
        String festivalTitle,
        String festivalTitleEn,
        String content,
        DiaryVisibility visibility,
        List<String> photoUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isOwner,
        String authorNickname
) {
    public static FestivalDiaryResponseDto of(FestivalDiary diary, List<String> photoUrls, boolean isOwner, String authorNickname) {
        return new FestivalDiaryResponseDto(
                diary.getId(),
                diary.getFestivalId(),
                diary.getFestivalTitle(),
                diary.getFestivalTitleEn() != null ? diary.getFestivalTitleEn() : "",
                diary.getContent(),
                diary.getVisibility(),
                photoUrls,
                diary.getCreatedAt(),
                diary.getUpdatedAt(),
                isOwner,
                authorNickname
        );
    }
}
