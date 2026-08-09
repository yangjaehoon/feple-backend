package com.feple.feple_backend.admin.ocr;

import java.util.List;

public record TimetableOcrApplyResultDto(
        int savedCount,
        int failedCount,
        List<TimetableOcrFailure> failures) {
}
