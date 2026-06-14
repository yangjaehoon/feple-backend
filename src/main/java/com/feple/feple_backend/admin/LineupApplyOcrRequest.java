package com.feple.feple_backend.admin;

import java.util.List;

public record LineupApplyOcrRequest(Long festivalId, List<Long> artistIds) {}
