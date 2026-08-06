package com.feple.feple_backend.post.dto;

import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.PostDraft;
import java.time.LocalDateTime;
import java.util.List;

public record PostDraftResponseDto(
        String title,
        String content,
        BoardType boardType,
        boolean anonymous,
        List<String> imageUrls,
        Long artistId,
        Long festivalId,
        LocalDateTime updatedAt
) {
    public static PostDraftResponseDto from(PostDraft draft) {
        return new PostDraftResponseDto(
                draft.getTitle(),
                draft.getContent(),
                draft.getBoardType(),
                draft.isAnonymous(),
                draft.getImageKeys(),
                draft.getArtistId(),
                draft.getFestivalId(),
                draft.getUpdatedAt()
        );
    }
}
