package com.feple.feple_backend.post.dto;

import com.feple.feple_backend.post.entity.ReportReason;

public record ReportSubmitRequest(ReportReason reason, String detail) {}
