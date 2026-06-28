package com.feple.feple_backend.admin.ocr;

import java.util.List;

public record OcrApplyRequestDto(
        Long festivalId,
        List<OcrResultDto> entries) {
}
