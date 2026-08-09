package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.AdminParamDefaults;

record SetlistRequestListParams(String status, Integer page, String keyword) {
    SetlistRequestListParams {
        status  = AdminParamDefaults.orDefault(status, AdminConstants.STATUS_PENDING);
        page    = AdminParamDefaults.orZero(page);
        keyword = AdminParamDefaults.orEmpty(keyword);
    }
}
