package com.feple.feple_backend.admin.point;

import com.feple.feple_backend.admin.AdminParamDefaults;

record PointListParams(Integer page, String keyword) {

    PointListParams {
        page = Math.max(0, AdminParamDefaults.orZero(page));
        keyword = AdminParamDefaults.orEmpty(keyword);
    }
}
