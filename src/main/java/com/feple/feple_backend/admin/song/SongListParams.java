package com.feple.feple_backend.admin.song;

import com.feple.feple_backend.admin.support.AdminConstants;
import com.feple.feple_backend.admin.support.AdminParamDefaults;

record SongListParams(String status, Integer page, String keyword) {
    SongListParams {
        status  = AdminParamDefaults.orDefault(status, AdminConstants.STATUS_PENDING);
        page    = AdminParamDefaults.orZero(page);
        keyword = AdminParamDefaults.orEmpty(keyword);
    }
}
