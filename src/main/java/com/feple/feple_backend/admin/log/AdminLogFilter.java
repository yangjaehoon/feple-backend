package com.feple.feple_backend.admin.log;

import java.time.LocalDate;

public record AdminLogFilter(
        String targetType,
        String adminUsername,
        LocalDate from,
        LocalDate to
) {}
