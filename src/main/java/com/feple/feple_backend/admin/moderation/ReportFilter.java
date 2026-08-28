package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.support.AdminConstants;
import com.feple.feple_backend.admin.support.AdminParamDefaults;
import com.feple.feple_backend.global.ReportTypes;

record ReportFilter(String type, String status, Integer page, String keyword) {
    ReportFilter {
        type    = AdminParamDefaults.orDefaultIfBlank(type, ReportTypes.POST);
        status  = AdminParamDefaults.orDefaultIfBlank(status, AdminConstants.STATUS_PENDING);
        keyword = AdminParamDefaults.orEmpty(keyword);
        page    = AdminParamDefaults.orZero(page);
    }
}
