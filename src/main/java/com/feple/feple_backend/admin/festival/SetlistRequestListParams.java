package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.AdminConstants;

record SetlistRequestListParams(String status, Integer page, String keyword) {
    SetlistRequestListParams {
        status  = status == null ? AdminConstants.STATUS_PENDING : status;
        page    = page == null ? 0 : page;
        keyword = keyword == null ? "" : keyword;
    }
}
