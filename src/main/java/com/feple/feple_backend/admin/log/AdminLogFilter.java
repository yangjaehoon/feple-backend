package com.feple.feple_backend.admin.log;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record AdminLogFilter(
        String targetType,
        String adminUsername,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Integer page
) {
    public AdminLogFilter {
        targetType    = targetType == null ? "" : targetType;
        adminUsername = adminUsername == null ? "" : adminUsername;
        // 음수 page는 PageRequest.of()가 IllegalArgumentException을 던지므로(URL 직접 수정 방어) 0으로 정규화.
        page = Math.max(0, page == null ? 0 : page);
    }
}
