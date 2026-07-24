package com.feple.feple_backend.admin.user;

import com.feple.feple_backend.admin.AdminParamDefaults;

record UserListFilter(String filter, String sort, Integer page, String keyword) {
    UserListFilter {
        filter  = AdminParamDefaults.orEmpty(filter);
        sort    = AdminParamDefaults.orDefault(sort, "latest");
        keyword = AdminParamDefaults.orEmpty(keyword);
        page    = AdminParamDefaults.orZero(page);
    }
}
