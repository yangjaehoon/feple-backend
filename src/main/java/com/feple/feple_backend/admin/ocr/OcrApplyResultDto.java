package com.feple.feple_backend.admin.ocr;

import java.util.List;

public record OcrApplyResultDto(
        int savedCount,
        int failedCount,
        List<OcrFailure> failures) {
}
