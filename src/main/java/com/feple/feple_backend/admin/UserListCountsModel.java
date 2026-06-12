package com.feple.feple_backend.admin;

import java.util.Map;

public record UserListCountsModel(
        Map<Long, Long> reportCounts,
        Map<Long, Long> postCounts,
        Map<Long, Long> commentCounts
) {}
