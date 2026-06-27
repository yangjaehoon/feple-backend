package com.feple.feple_backend.admin;

import java.util.List;

public record OcrApplyRequestDto(
        Long festivalId,
        List<OcrResultDto> entries) {
}
