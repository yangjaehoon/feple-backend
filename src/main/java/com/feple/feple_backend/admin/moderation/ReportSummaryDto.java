package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.post.entity.ReportReason;
import java.time.LocalDateTime;

public record ReportSummaryDto(
        Long id,
        Long postId,
        String postTitle,
        ReportReason reason,
        LocalDateTime createdAt
) {
    public static ReportSummaryDto from(com.feple.feple_backend.post.entity.PostReport report) {
        return new ReportSummaryDto(report.getId(), report.getPostId(), report.getPostTitle(), report.getReason(), report.getCreatedAt());
    }
}
