package com.feple.feple_backend.admin;

import java.util.List;
import java.util.Map;

public record OcrApplyResultDto(
        int savedCount,
        int failedCount,
        List<Map<String, String>> failures) {
}
