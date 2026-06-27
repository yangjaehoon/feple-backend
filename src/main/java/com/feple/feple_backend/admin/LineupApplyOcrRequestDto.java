package com.feple.feple_backend.admin;

import java.util.List;

public record LineupApplyOcrRequestDto(Long festivalId, List<Long> artistIds) {}
