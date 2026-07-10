package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.post.entity.ReportReason;
import java.time.LocalDateTime;

public record PostReportSummaryDto(
        Long id,
        Long postId,
        String postTitle,
        ReportReason reason,
        LocalDateTime createdAt
) {
    public static PostReportSummaryDto from(com.feple.feple_backend.post.entity.PostReport report) {
        return new PostReportSummaryDto(report.getId(), report.getPostId(), report.getPostTitle(), report.getReason(), report.getCreatedAt());
    }
}
