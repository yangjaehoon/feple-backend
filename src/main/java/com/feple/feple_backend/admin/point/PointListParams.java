package com.feple.feple_backend.admin.point;

import com.feple.feple_backend.admin.support.AdminParamDefaults;

record PointListParams(Integer page, String keyword) {

    PointListParams {
        page = AdminParamDefaults.pageOrFirst(page);
        keyword = AdminParamDefaults.orEmpty(keyword);
    }
}
