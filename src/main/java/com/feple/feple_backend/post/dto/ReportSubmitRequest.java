package com.feple.feple_backend.post.dto;

import com.feple.feple_backend.post.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportSubmitRequest(
        @NotNull(message = "신고 사유를 선택해주세요.") ReportReason reason,
        @Size(max = 500, message = "신고 사유는 500자 이하로 입력해주세요.") String detail) {}
