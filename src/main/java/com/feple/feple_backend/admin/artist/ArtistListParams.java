package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.AdminParamDefaults;
import com.feple.feple_backend.admin.AdminUrlUtils;

record ArtistListParams(Integer page, String keyword, String sort) {

    ArtistListParams {
        page = AdminParamDefaults.orZero(page);
        keyword = AdminParamDefaults.orEmpty(keyword);
        sort = AdminParamDefaults.orEmpty(sort);
    }

    String toRedirectUrl() {
        return AdminUrlUtils.listUrl("/admin/artists", "page", page, "keyword", keyword, "sort", sort);
    }
}
