package com.feple.feple_backend.admin.user;

import java.util.Map;

public record UserListCountsDto(
        Map<Long, Long> reportCounts,
        Map<Long, Long> postCounts,
        Map<Long, Long> commentCounts
) {}
