package com.feple.feple_backend.admin.ocr;

import java.util.List;

public record LineupOcrApplyRequestDto(Long festivalId, List<Long> artistIds, List<String> unmatchedNames) {}
