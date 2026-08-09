package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.AdminParamDefaults;

record SetlistChangeRequestListParams(String status, Integer page, String keyword) {
    SetlistChangeRequestListParams {
        status  = AdminParamDefaults.orDefault(status, AdminConstants.STATUS_PENDING);
        page    = AdminParamDefaults.orZero(page);
        keyword = AdminParamDefaults.orEmpty(keyword);
    }
}
