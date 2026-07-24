package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.AdminParamDefaults;

record ReportFilter(String type, String status, Integer page, String keyword) {
    ReportFilter {
        type    = AdminParamDefaults.orDefaultIfBlank(type, AdminConstants.REPORT_TYPE_POST);
        status  = AdminParamDefaults.orDefaultIfBlank(status, AdminConstants.STATUS_PENDING);
        keyword = AdminParamDefaults.orEmpty(keyword);
        page    = AdminParamDefaults.orZero(page);
    }
}
