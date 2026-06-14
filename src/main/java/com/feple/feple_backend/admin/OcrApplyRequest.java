package com.feple.feple_backend.admin;

import java.util.List;

public record OcrApplyRequest(
        Long festivalId,
        List<OcrResultDto> entries) {
}
