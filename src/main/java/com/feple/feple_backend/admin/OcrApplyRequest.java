package com.feple.feple_backend.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public record OcrApplyRequest(
        Long festivalId,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate festivalDate,
        List<OcrResultDto> entries) {
}
