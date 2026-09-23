package com.feple.feple_backend.admin.log;

import com.feple.feple_backend.admin.support.AdminParamDefaults;
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
        targetType    = AdminParamDefaults.orEmpty(targetType);
        adminUsername = AdminParamDefaults.orEmpty(adminUsername);
        page          = AdminParamDefaults.pageOrFirst(page);
    }
}
