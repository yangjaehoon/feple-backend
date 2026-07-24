package com.feple.feple_backend.admin.certification;

import com.feple.feple_backend.admin.AdminParamDefaults;

record CertificationFilter(String status, Integer page, String keyword) {
    CertificationFilter {
        status  = AdminParamDefaults.orEmpty(status);
        page    = AdminParamDefaults.orZero(page);
        keyword = AdminParamDefaults.orEmpty(keyword);
    }
}
