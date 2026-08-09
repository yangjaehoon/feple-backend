package com.feple.feple_backend.admin.ocr;

import java.util.List;

public record TimetableOcrApplyRequestDto(
        Long festivalId,
        List<TimetableOcrResultDto> entries) {
}
