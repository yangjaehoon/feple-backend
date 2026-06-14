package com.feple.feple_backend.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrResultDto(
        String artist,
        String stage,
        String date,
        String startTime,
        String endTime,
        Integer confidence) {
}
