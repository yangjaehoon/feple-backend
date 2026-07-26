package com.feple.feple_backend.user.dto;

import com.feple.feple_backend.user.entity.PointReason;
import com.feple.feple_backend.user.entity.UserPointLog;
import java.time.LocalDateTime;

public record PointLogResponseDto(
        Long id,
        Long userId,
        String userNickname,
        int delta,
        PointReason reason,
        Long refId,
        String note,
        boolean linksToPost,
        boolean linksToCertification,
        LocalDateTime createdAt
) {
    public static PointLogResponseDto from(UserPointLog log) {
        PointReason reason = log.getReason();
        boolean linksToPost = reason == PointReason.POST_CREATED
                || reason == PointReason.COMMENT_CREATED
                || reason == PointReason.POST_LIKED_RECEIVED;
        boolean linksToCertification = reason == PointReason.CERT_APPROVED;
        return new PointLogResponseDto(
                log.getId(), log.getUserId(), log.getUserNickname(), log.getDelta(), reason, log.getRefId(), log.getNote(),
                linksToPost, linksToCertification, log.getCreatedAt());
    }
}
