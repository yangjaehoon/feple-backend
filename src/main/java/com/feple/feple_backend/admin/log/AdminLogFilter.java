package com.feple.feple_backend.admin.log;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AdminLogFilter(
        String targetType,
        String adminUsername,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
    public AdminLogFilter {
        targetType    = targetType == null ? "" : targetType;
        adminUsername = adminUsername == null ? "" : adminUsername;
    }
}
