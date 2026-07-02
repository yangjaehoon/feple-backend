package com.feple.feple_backend.post.dto;

import com.feple.feple_backend.post.entity.ReportReason;

public record SubmitReportCommand(ReportReason reason, String detail) {}
