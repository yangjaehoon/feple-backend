package com.feple.feple_backend.admin.ocr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LineupRawResult(String name, Integer confidence) {
}
